(ns marauder.map)

(def ^{:doc "Map for later reference."
       :dynamic true
       :private true}
  *the-map*
  nil)

(def ^{:doc "Everything tracked."
       :private true}
  state
  (atom {:users
         {7 {:name "Boston" :lat 42.369706 :lng -71.060257 :mark nil}
          8 {:name "home"   :lat 42.382545 :lng -71.137122 :mark nil}
          9 {:name "work"   :lat 42.382314 :lng -71.137525 :mark nil}}
         :places
         {}}))

(defn mark-user [m u]
  "Return user u with a mark in map m."
  (let [mark (google.maps.Marker.
              (clj->js {:title (:name u)
                        :position (google.maps.LatLng. (:lat u) (:lng u))
                        ;; :animation google.maps.Animation.DROP
                        :map m}))]
    (assoc u :mark mark)))

(defn place-markers
  "Return state s with a marker on map m for each user."
  [s m]
  (assoc s :users
         (into {}
               (map (fn [id user] [id (mark-user m user)]) (:users s)))))

(defn bound-marks
  [marks]
  (let [result (google.maps.LatLngBounds.)]
    (doseq [m marks] (.extend result (.-position m)))
    result))

(defn usa-ma-boston []
  (google.maps.LatLng. 42.369706 -71.060257))

(defn australia-sydney []
  (google.maps.LatLng. -33.859972 151.211111))

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [options {:center (usa-ma-boston)
                 ;; :center (australia-sydney)
                 :zoom 8
                 :mapTypeId google.maps.MapTypeId.ROADMAP}
        the-map (google.maps.Map. (.getElementById js/document "map-canvas")
                                  (clj->js options))]
    (place-markers @state the-map)
    '(swap! state place-markers the-map)
    '(.fitBounds the-map
                (bound-marks
                 (map :mark (vals (:users
                                   (swap! state place-markers the-map))))))
    (set! *the-map* the-map)))

(google.maps.event.addDomListener js/window "load" initialize)
