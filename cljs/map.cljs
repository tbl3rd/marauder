(ns marauder.map)

(def ^{:doc "Map for later reference."
       :dynamic true
       :private true}
  *the-map*
  nil)

(def ^{:doc "Center of map for later reference."
       :dynamic true
       :private true}
  *the-center*
  nil)

(defn boston []
  (google.maps.LatLng. 42.369706 -71.060257))

(def map-type
  {:roadmap   google.maps.MapTypeId.ROADMAP
   :satellite google.maps.MapTypeId.SATELLITE
   :hybrid    google.maps.MapTypeId.HYBRID
   :terrain   google.maps.MapTypeId.TERRAIN})

(defn initialize
  "Open a ROADMAP on Boston in :div#map-canvas."
  []
  (let [options {:center (boston)
                 :zoom 8
                 :mapTypeId google.maps.MapTypeId.ROADMAP}
        the-map (google.maps.Map. (.getElementById js/document "map-canvas")
                                  (clj->js options))
        the-center (google.maps.Marker.
                    (clj->js {:position (.getCenter the-map)
                              :map the-map
                              :title "click to zoom"}))
        recenter (fn [] (.panTo the-map (.getPosition the-center)))
        rezoom (fn [] (doto the-map
                        (.setZoom 8)
                        (-> the-center .getPosition .setCenter)))]
    (.addListener the-map "center_changed"
                  #(.setTimeout js/window recenter 3000))
    (google.maps.event.addListener the-center "click" rezoom)
    (set! *the-map* the-map)
    (set! *the-center* the-center)))

(google.maps.event.addDomListener js/window "load" initialize)
