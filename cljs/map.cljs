(ns marauder.map
  (:require [cljs.reader :as reader]
            [marauder.icon :as icon]
            [goog.events :as events]
            [goog.net.XhrIo :as xhr]))

(defn usa-ma-boston []
  (google.maps.LatLng. 42.369706 -71.060257))

(defn australia-sydney []
  (google.maps.LatLng. -33.859972 151.211111))

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
    (let [latlng (google.maps.LatLng. (:lat update) (:lng update))
          name (:name update)]
      {id
       (if-let [mark (get the-marks id)]
         (doto mark
           (.setPosition latlng)
           (.setTitle name))
         (google.maps.Marker.
          (clj->js {:title name
                    :icon (icon/get-icon)
                    :position latlng
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

(defn send-update-and-handle-response
  "Send request map to /update then pass response to handle-response."
  [request handle-response]
  (let [connection (goog.net.XhrIo.)]
    (events/listen connection goog.net.EventType/COMPLETE
                   #(handle-response
                     (reader/read-string (.getResponseText connection))))
    (.send connection "/update" "POST" request
           (clj->js {"Content-type" "application/edn"}))))

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [my-map (make-my-map)
        request {:id nil :name "tbl" :lat 42.386951 :lng -71.057038}
        handler (fn [response]
                  (swap! my-marks update-the-user-marks my-map response))
        response (send-update-and-handle-response request handler)
        [id you] (:you response)]
    (set! *my-id* id)
    (google.maps.event.addListenerOnce
     my-map "idle" (fn []
                     (show-all-marks @my-marks)
                     (set! *my-map* my-map)))))

(google.maps.event.addDomListener js/window "load" initialize)

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
   :places {}})
