(ns marauder.map
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.icon :as icon]))

(defn log
  "Log stuff on the JS console.  Maps do well."
  [stuff]
  (.. js/window -console (log (clj->js stuff))))

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

(def ^{:doc "A google.maps.Map that gets set once in initialize."}
  my-map (atom nil))

(def ^{:doc "The google.maps.Marker objects indexed by id."}
  marks (atom {}))

(def ^{:doc "Local user state cached across sessions."}
  state (atom (cljs.reader/read-string
               (or (. js/localStorage getItem "state")
                   (pr-str {:id nil :name "anonymous"})))))

(defn remember!
  "Remember that key k has value v across sessions."
  [k v]
  (log {k v})
  (let [value (swap! state assoc k v)]
    (. js/localStorage setItem "state" (pr-str value))))

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

(def ^{:doc "The maximum Z index established by raise! so far."}
  max-z-index-for-raise
  (atom google.maps.MAX_ZINDEX))
(defn raise!
  [thing]
  (. thing setZIndex (swap! max-z-index-for-raise inc)))

(defn bound-response
  "Get the LatLngBounds of all the positions in the update response."
  [response]
  (let [bounds (new google.maps.LatLngBounds)
        users (vals (:users response))]
    (doseq [position (map glatlng users)] (. bounds extend position))
    bounds))

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

(defn update-my-position!
  "Update my state with position."
  [position]
  (swap! state merge
         {:lat (latitude position)
          :lng (longitude position)}))

(defn request-update
  "Request an update from the server and (handle response)."
  [handle]
  (-> js/navigator
      (. -geolocation)
      (. getCurrentPosition
         (fn [position]
           (post "/update"
                 (select-keys (update-my-position! position)
                              [:id :name :lat :lng])
                 handle)))))

(defn make-google-map
  "A Google Map covering the region defined by bounds."
  [bounds]
  (doto (new google.maps.Map
             (. js/document getElementById "googlemapcanvas")
             (clj->js {:center (. bounds getCenter)
                       :mapTypeId google.maps.MapTypeId.ROADMAP}))
    (.fitBounds bounds)))

(defn open-info
  "Open an info window on gmap for mark."
  [gmap mark name]
  (let [info (new google.maps.InfoWindow (clj->js {:content name}))]
    (raise! info)
    (. info open gmap mark)
    (after #(. info close) 30000)
    (reverse-geocode mark
                     (fn [address]
                       (. info setContent
                          (str name " @<br>" address))))))

(defn mark-map
  "Mark gmap for user with id."
  [gmap id user]
  (log {:call "mark-map" :gmap gmap :id id :user user :state (:id @state)})
  (log {:equal (= id (:id @state))})
  (let [name (:name user)
        icon (if (= id (:id @state))
               (icon/get-icon-for-me)
               (icon/get-icon-for name))
        mark (new google.maps.Marker
                  (clj->js {:title name
                            :icon icon
                            :position (glatlng user)
                            :animation google.maps.Animation.DROP
                            :map gmap}))]
    (google.maps.event.addListener mark "click" #(open-info gmap mark name))
    mark))

(defn update-user
  "Update marks for user with id.  Return the user's mark."
  [id user]
  (if-let [mark (get @marks id)]
    (let [name (:name user)]
      (doto mark
        (.setPosition (glatlng user))
        (.setTitle name)))
    (let [mark (mark-map @my-map id user)]
      (swap! marks (fn [m] (assoc m id mark)))
      mark)))

(defn update-user-marks
  "Update user marks on map."
  []
  (request-update
   (fn [response]
     (log {"update-user-marks" (count (:users response))})
     (doseq [[id user] (:users response)] (update-user id user)))))

(defn position-map-controls
  [gmap]
  (let [controls (. gmap -controls)
        right-top (aget controls google.maps.ControlPosition.RIGHT-BOTTOM)
        div (. js/document getElementById "marauder-controls")]
    (log {:f "position-map-controls" :div div})
    (. right-top push div)))

(defn initialize
  "Open a ROADMAP on Boston in :div#googlemapcanvas with everyone marked."
  []
  (request-update
   (fn [response]
     (log {"initialize" (count (:users response))})
     (log (pr-str response))
     (remember! :id (first (keys (:you response))))
     (swap! my-map #(make-google-map (bound-response response)))
     (google.maps.event.addListenerOnce
      @my-map "idle" #(periodically update-user-marks 60000))
     (position-map-controls @my-map)
     (letfn [(mark [[id user]] [id (mark-map @my-map id user)])]
       (swap! marks (fn [m] (into m (map mark (:users response)))))))))

(google.maps.event.addDomListener js/window "load" initialize)
