(ns marauder.map
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.icon :as icon]
            [goog.events :as events]
            [goog.net.cookies :as cookies-but-who-cares?]
            [goog.net.XhrIo :as xhrio-but-who-cares?]))

(def ^{:doc "A google.maps.Map for later reference."
       :dynamic true}
  *my-map*
  nil)

(def ^{:doc "The google.maps.Marker objects on *my-map* by id."}
  my-marks (atom {}))

(def ^{:doc "My ID from the server."}
  state
  (atom (cljs.reader/read-string
         (or (. js/localStorage getItem "state")
             (pr-str {:id nil :name "anonymous"})))))

(defn set-my!
  [k v]
  (let [value (swap! state assoc k v)]
    (. js/localStorage setItem "state" (pr-str value))))

(defn log
  "Log stuff on the JS console."
  [stuff]
  (. js/console log (clj->js stuff)))

;; Hide differences between different geoposition types.
;;
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

(defn make-my-map [response]
  (let [options {:center (australia-sydney)
                 :zoom 8
                 :mapTypeId google.maps.MapTypeId.ROADMAP}]
    (new google.maps.Map
         (. js/document getElementById "map-canvas")
         (clj->js options))))

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
                    :position position
                    :map the-map})))})))

(defn bound-response
  "Get the LatLngBounds of all the positions in the update response."
  [response]
  (let [bounds (new google.maps.LatLngBounds)
        users (vals (:users response))]
    (doseq [position (map glatlng users)] (. bounds extend position))
    bounds))

(defn show-all-marks
  "Show all the marks on their map."
  [marks]
  (let [bounds (google.maps.LatLngBounds.)]
    (doseq [mark (vals marks)] (. bounds extend (. mark -position)))
    (if-let [the-map (map-of-the-marks marks)]
      (. the-map fitBounds bounds))
    bounds))

(defn update-the-user-marks
  "Update the-marks on the-map with the :users response."
  [the-marks the-map response]
  (apply merge the-marks
         (map (make-user-mark-updater the-map the-marks) (:users response))))

(defn send-await-response
  "Send request map to /update then pass response to handle-response."
  [request handle-response]
  (let [connection (new goog.net.XhrIo)]
    (events/listen connection goog.net.EventType/COMPLETE
                   #(handle-response
                     (cljs.reader/read-string
                      (. connection getResponseText))))
    (. connection send "/update" "POST" request
       (clj->js {"Content-type" "application/edn"}))))

(defn update-my-position
  [my-marks my-map id name]
  (let [handle (fn [response]
                 (if (not id) (set-my! :id (first (keys (:you response)))))
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
        (. -geolocation)
        (. getCurrentPosition update))))

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [my-map (make-my-map nil)]
    (update-my-position my-marks my-map (:id @state) (:name @state))
    (google.maps.event.addListenerOnce
     my-map "idle" (fn []
                     (log "initialize idle")
                     (set! *my-map* my-map)))))

(google.maps.event.addDomListener js/window "load" initialize)
