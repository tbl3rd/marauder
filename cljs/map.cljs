(ns marauder.map)

(defn usa-ma-boston []
  (google.maps.LatLng. 42.369706 -71.060257))

(defn australia-sydney []
  (google.maps.LatLng. -33.859972 151.211111))

(defn make-the-map []
  (let [options {:center (usa-ma-boston)
                 ;; :center (australia-sydney)
                 :zoom 8
                 :mapTypeId google.maps.MapTypeId.ROADMAP}]
    (google.maps.Map. (.getElementById js/document "map-canvas")
                      (clj->js options))))

(def ^{:doc "Map for later reference."
       :dynamic true}
  *the-map*
  nil)

(defn initial-state []
  {:users {"Boston"    {:lat 42.369706 :lng -71.060257}
           "Cambridge" {:lat 42.378836 :lng -71.110436}
           "home"      {:lat 42.382545 :lng -71.137122}
           "work"      {:lat 42.366931 :lng -71.091352}}
   :places {}})

(def ^{:doc "Everything tracked."}
  state
  (atom (initial-state)))

(defn mark-user
  "Return state s with a marker on map m for user [name info]."
  [s m name]
  (let [info (get-in s [:users name])
        latlng (google.maps.LatLng. (:lat info) (:lng info))]
    (assoc-in s [:users name :mark]
              (google.maps.Marker.
               (clj->js {:title name :position latlng :map m})))))

(defn place-marker!
  [s m name]
  (swap! s mark-user m name))

(defn bound-marks
  [marks]
  (let [result (google.maps.LatLngBounds.)]
    (doseq [m marks] (.extend result (.-position m)))
    result))

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [the-map (make-the-map)]
    (doseq [name (keys (:users @state))] (place-marker! state the-map name))
    (.fitBounds the-map (bound-marks (map :mark (vals (:users @state)))))
    (set! *the-map* the-map)))

(google.maps.event.addDomListener js/window "load" initialize)
