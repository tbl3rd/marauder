(ns marauder.util
  (:require [cljs.reader :as reader-but-who-cares?]))

(defn log
  "Log stuff on the JS console.  Clojure maps look good."
  [stuff]
  (.. js/window -console (log (clj->js stuff))))

(defn by-dom-id
  "The dom element identified by dom-id."
  [dom-id]
  (. js/document getElementById (name dom-id)))

(defn add-listener
  "Call f when event fires on div."
  [div event f]
  (google.maps.event.addDomListener div (name event) f))

(defn add-listener-once
  "Call f when event fires on obj the first time."
  [obj event f]
  (google.maps.event.addListenerOnce obj (name event) f))

;; Hide differences between GoogleMaps and HTML5 geoposition types.
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

(defn mlatlng
  "Return a {:keys [lat lng]} from some position value."
  [position]
  {:lat (latitude position)
   :lng (longitude position)})

(def ^{:doc "A google.maps.Geocoder set once from reverse-geocode."}
  geocoder (atom nil))

(defn reverse-geocode
  "Call handle with the address of mark."
  [mark handle]
  (if (nil? @geocoder)
    (swap! geocoder (constantly (new google.maps.Geocoder))))
  (. @geocoder geocode (clj->js {:latLng (. mark getPosition)})
     (fn [results status]
       (if (= status google.maps.GeocoderStatus.OK)
         (if-let [result (aget results 0)]
           (handle (. result -formatted-address)))))))

(defn after
  "Call f after ms milliseconds."
  [f ms]
  (js/setTimeout f ms))

(defn periodically
  "After ms milliseconds call f every ms milliseconds."
  [f ms]
  (after (fn [] (f) (periodically f ms)) ms))

(def ^{:doc "The maximum Z index established by raise so far."
       :private true}
  max-z-index-for-raise
  (atom google.maps.MAX_ZINDEX))

(defn raise
  [thing]
  (. thing setZIndex (swap! max-z-index-for-raise inc)))

(defn post
  "POST request map to uri then pass response to handle-response."
  [uri request handle-response]
  (let [connection (new goog.net.XhrIo)]
    (goog.events.listen connection goog.net.EventType/COMPLETE
                        #(handle-response
                          (cljs.reader/read-string
                           (. connection getResponseText))))
    (. connection send uri "POST" request
       (clj->js {"Content-type" "application/edn"}))))

(defn new-marker
  "New marker at position on gmap with icon and title."
  [gmap position icon title]
  (new google.maps.Marker
       (clj->js {:map gmap
                 :position position
                 :icon icon
                 :title title
                 :animation google.maps.Animation.DROP})))
