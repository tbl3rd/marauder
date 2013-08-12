(ns marauder.map)

(defn usa-ma-boston []
  (google.maps.LatLng. 42.369706 -71.060257))

(defn australia-sydney []
  (google.maps.LatLng. -33.859972 151.211111))

(defn make-the-map []
  (let [options {:center (usa-ma-boston)
                 :zoom 8
                 :mapTypeId google.maps.MapTypeId.ROADMAP}]
    (google.maps.Map. (.getElementById js/document "map-canvas")
                      (clj->js options))))

(def ^{:doc "Map for later reference."
       :dynamic true}
  *the-map*
  nil)

(defn initial-state []
  {:users {"6" {:name "Boston"    :lat 42.369706 :lng -71.060257}
           "7" {:name "Cambridge" :lat 42.378836 :lng -71.110436}
           "8" {:name "home"      :lat 42.382545 :lng -71.137122}
           "9" {:name "work"      :lat 42.366931 :lng -71.091352}}
   :places {}})

(def ^{:doc "Everything tracked."}
  state
  (atom (initial-state)))

(defn mark-user
  "Return state s with a marker on map m for user [name info]."
  [s m id]
  (let [info (get-in s [:users id])
        latlng (google.maps.LatLng. (:lat info) (:lng info))]
    (assoc-in s [:users id :mark]
              (google.maps.Marker.
               (clj->js {:title (:name info) :position latlng :map m})))))

(defn bound-marks
  "Get the bounding box for all marks."
  [marks]
  (let [result (google.maps.LatLngBounds.)]
    (doseq [m marks] (.extend result (.-position m)))
    result))

(defn show-all-on-map
  "Get all places in the state s onto the map m."
  [m s]
  (.fitBounds m (bound-marks (map :mark (vals (:users s))))))

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [the-map (make-the-map)]
    (doseq [id (keys (:users @state))]
      (swap! state mark-user the-map id))
    (show-all-on-map the-map @state)
    (set! *the-map* the-map)))

(google.maps.event.addDomListener js/window "load" initialize)
