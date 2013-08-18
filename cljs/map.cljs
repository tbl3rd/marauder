(ns marauder.map
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.icon :as icon]
            [goog.events :as events]
            [goog.net.cookies :as cookies-but-who-cares?]
            [goog.net.XhrIo :as xhrio-but-who-cares?]))

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
(extend-protocol ILatitudeLongitude     ; HACK around null exception.
  nil
  (latitude [p] 0)
  (longitude [p] 0))

(def ^{:doc "A google.maps.Map."}
  my-map (atom nil))

(def ^{:doc "The google.maps.Marker objects indexed by id."}
  my-marks (atom {}))

(def ^{:doc "User state cached across sessions."}
  state (atom (cljs.reader/read-string
               (or (. js/localStorage getItem "state")
                   (pr-str {:id nil :name "anonymous"})))))

(defn remember!
  "Remember that key k has value v across sessions."
  [k v]
  (let [value (swap! state assoc k v)]
    (. js/localStorage setItem "state" (pr-str value))))

(defn log
  "Log stuff on the JS console."
  [stuff]
  (. js/console log (clj->js stuff)))

(defn glatlng
  "A Google Maps LatLng from whatever."
  ([lat lng]
     (new google.maps.LatLng lat lng))
  ([position]
     (if (map? position)
       (glatlng (:lat position) (:lng position))
       (glatlng (latitude position) (longitude position)))))

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
    (events/listen connection goog.net.EventType/COMPLETE
                   #(handle-response
                     (cljs.reader/read-string
                      (. connection getResponseText))))
    (. connection send uri "POST" request
       (clj->js {"Content-type" "application/edn"}))))

(defn call-with-new-position
  "Pass new position to with-new-position."
  [with-new-position]
  (-> js/navigator
      (. -geolocation)
      (. getCurrentPosition with-new-position)))

(defn update-my-position!
  "Update my state with position."
  [position]
  (log "update-my-position! callback")
  (log {:lat (latitude position) :lng (longitude position)})
  (swap! state merge
         {:lat (latitude position)
          :lng (longitude position)}))

(defn request-update
  "Request an update from the server and (handle update)."
  [state handle]
  (call-with-new-position
   (fn [position]
     (post "/update"
           (select-keys (update-my-position! position)
                        [:id :name :lat :lng])
           handle))))

(defn make-google-map
  "A Google Map covering the region defined by bounds."
  [bounds]
  (doto (new google.maps.Map
             (. js/document getElementById "googlemapcanvas")
             (clj->js {:center (. bounds getCenter)
                       :mapTypeId google.maps.MapTypeId.ROADMAP}))
    (.fitBounds bounds)))

(defn mark-map
  "Mark gmap with title at position."
  [gmap title position]
  (new google.maps.Marker
       (clj->js {:title title
                 :position position
                 :map gmap})))

(defn initialize
  "Open a ROADMAP on Boston in :div#googlemapcanvas with everyone marked."
  []
  (request-update
   state
   (fn [response]
     (log "initialize")
     (log (pr-str response))
     (remember! :id (first (keys (:you response))))
     (swap! my-map
            (constantly
             (make-google-map
              (bound-response (js->clj response)))))
     (letfn [(mark [[id user]]
               [id (mark-map @my-map (:name user) (glatlng user))])]
       (swap! my-marks
              (fn [m] (into m (map mark (:users response)))))))))

(google.maps.event.addDomListener js/window "load" initialize)
