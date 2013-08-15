(ns marauder.map
  (:require [cljs.reader :as reader]
            [marauder.icon :as icon]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]))

(defn log
  "Log stuff on the JS console."
  [stuff]
  (.log js/console (clj->js stuff)))

(defprotocol ILatitudeLongitude
  (latitude [p])
  (longitude [p]))

(extend-type google.maps.LatLng
  ILatitudeLongitude
  (latitude [p] (.lat p))
  (longitude [p] (.lng p)))

(extend-protocol ILatitudeLongitude
  object
  (latitude [p] (.. p -coords -latitude))
  (longitude [p] (.. p -coords -longitude)))

(defn glatlng
  "A Google Maps LatLng from whatever."
  ([lat lng]
     (new google.maps.LatLng lat lng))
  ([position]
     (if (map? position)
       (glatlng (:lat position) (:lng position))
       (glatlng (latitude position) (longitude position)))))

(defn usa-ma-boston [] (glatlng 42.369706 -71.060257))

(defn australia-sydney [] (glatlng -33.859972 151.211111))

(defn make-my-map []
  (let [options {:center (usa-ma-boston)
                 :zoom 8
                 :mapTypeId google.maps.MapTypeId.ROADMAP}]
    (google.maps.Map. (.getElementById js/document "map-canvas")
                      (clj->js options))))

(def ^{:doc "Map for later reference."
       :dynamic true}
  *my-map*
  nil)

(def ^{:doc "All the markers on *my-map* indexed by id."
       :dynamic true}
  my-marks (atom {}))

(def ^{:doc "My ID from the server."
       :dynamic true}
  *my-id*
  nil)

(defn map-of-the-marks
  "Return nil or the map shared by the-marks."
  [the-marks]
  (first (map (fn [[id mark]] (.getMap mark)) the-marks)))

(defn make-user-mark-updater
  "A map of all the-marks on the-map."
  [the-map the-marks]
  (fn [[id update]]
    (let [position (glatlng update)
          name (:name update)]
      {id
       (if-let [mark (get the-marks id)]
         (doto mark
           (.setPosition position)
           (.setTitle name))
         (google.maps.Marker.
          (clj->js {:title name
                    :icon (icon/get-icon)
                    :position position
                    :map the-map})))})))

(defn show-all-marks
  "Show all the marks on their map."
  [marks]
  (let [bounds (google.maps.LatLngBounds.)]
    (doseq [mark (vals marks)] (.extend bounds (.-position mark)))
    (if-let [the-map (map-of-the-marks marks)]
      (.fitBounds the-map bounds))
    bounds))

(defn update-the-user-marks
  "Update the-marks on the-map with the :users response."
  [the-marks the-map response]
  (apply merge the-marks
         (map (make-user-mark-updater the-map the-marks) (:users response))))

(defn send-await-response
  "Send request map to /update then pass response to handle-response."
  [request handle-response]
  (let [connection (goog.net.XhrIo.)]
    (events/listen connection goog.net.EventType/COMPLETE
                   #(handle-response
                     (reader/read-string (.getResponseText connection))))
    (.send connection "/update" "POST" request
           (clj->js {"Content-type" "application/edn"}))))

(defn update-my-position
  [my-marks my-map id name]
  (let [handle (fn [response]
                 (if (not id) (set! *my-id* (first (keys (:you response)))))
                 (swap! my-marks update-the-user-marks my-map response)
                 (show-all-marks @my-marks))
        send (fn [update handle] (send-await-response update handle))
        update (fn [position]
                 (log position)
                 (send {:id id :name name
                        :lat (latitude position)
                        :lng (longitude position)}
                       handle)
                 )]
    (-> js/navigator
        (.-geolocation)
        (.getCurrentPosition update))))

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [my-map (make-my-map)]
    (update-my-position my-marks my-map *my-id* "tbl")
    (google.maps.event.addListenerOnce
     my-map "idle" (fn []
                     (log "initialize idle")
                     (set! *my-map* my-map)))))

(google.maps.event.addDomListener js/window "load" initialize)

(comment
  (def response
    {:you {"11ce549f-03f4-4307-b402-5bcaf649d85b" {:name "yang",
                                                   :lat 42.386951,
                                                   :lng -71.057038}},
     :users {"80043834-c7b7-4b1e-94b6-2afc83f2cbf4" {:name "tbl",
                                                     :lat 42.386951,
                                                     :lng -71.057038},
             "11ce549f-03f4-4307-b402-5bcaf649d85b" {:name "yang",
                                                     :lat 42.386951,
                                                     :lng -71.057038},
             "6" {:lng -71.060257,
                  :name "Boston",
                  :lat 42.369706},
             "7" {:lng -71.110436,
                  :name "Cambridge",
                  :lat 42.378836},
             "8" {:lng -71.137122,
                  :name "home",
                  :lat 42.382545},
             "9" {:lng -71.091352,
                  :name "work",
                  :lat 42.366931}},
     :places {}}))
