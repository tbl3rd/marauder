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
  my-marks (atom {}))

(def ^{:doc "Local user state cached across sessions."}
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
  (. (. js/window -console) log (clj->js stuff)))

(defn after
  "Call f after ms milliseconds."
  [f ms]
  (js/setTimeout f ms))

(defn periodically
  "After ms milliseconds call f every ms milliseconds."
  [f ms]
  (after (fn [] (f) (periodically f ms)) ms))

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

(defn update-my-position!
  "Update my state with position."
  [position]
  (log "update-my-position! callback")
  (log {:lat (latitude position) :lng (longitude position)})
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

(defn mark-map
  "Mark gmap with title at position."
  [gmap user]
  (new google.maps.Marker
       (clj->js {:title (:name user)
                 :position (glatlng user)
                 :map gmap})))

(defn update-user
  "Update my-marks for user with id.  Return the user's mark."
  [id user]
  (if-let [mark (get @my-marks id)]
    (doto mark
      (.setPosition (glatlng user))
      (.setTitle (:name user)))
    (let [mark (mark-map @my-map user)]
      (swap! my-marks (fn [m] (assoc m id mark)))
      mark)))

(defn update-user-marks
  "Update user marks on map."
  []
  (request-update
   (fn [response]
     (log {"update-user-marks callback" (count (:users response))})
     (doseq [[id user] (:users response)] (update-user id user)))))

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
      @my-map "idle" #(periodically update-user-marks 2000))
     (letfn [(mark [[id user]] [id (mark-map @my-map user)])]
       (swap! my-marks (fn [m] (into m (map mark (:users response)))))))))

(google.maps.event.addDomListener js/window "load" initialize)
