(ns marauder.map
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.icon :as icon]
            [marauder.util :as util]))

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
  (util/log {k v})
  (let [value (swap! state assoc k v)]
    (. js/localStorage setItem "state" (pr-str value))))

(defn bound-response
  "Get the LatLngBounds of all the positions in the update response."
  [response]
  (let [bounds (new google.maps.LatLngBounds)
        users (vals (:users response))]
    (doseq [position (map util/glatlng users)] (. bounds extend position))
    bounds))

(defn request-update
  "Request an update from the server and (handle response)."
  [handle]
  (-> js/navigator
      (. -geolocation)
      (. getCurrentPosition
         (fn [position]
           (util/post "/update"
                      (select-keys (swap! state merge (util/mlatlng position))
                                   [:id :name :lat :lng])
                      handle)))))

(defn make-google-map
  "A Google Map covering the region defined by bounds."
  [bounds]
  (doto (new google.maps.Map
             (util/by-dom-id :googlemapcanvas)
             (clj->js {:center (. bounds getCenter)
                       :mapTypeId google.maps.MapTypeId.ROADMAP}))
    (.fitBounds bounds)))

(defn open-info
  "Open an info window on gmap for mark."
  [gmap mark name]
  (let [info (new google.maps.InfoWindow (clj->js {:content name}))]
    (util/raise info)
    (. info open gmap mark)
    (util/after #(. info close) 30000)
    (util/reverse-geocode mark
                          (fn [address]
                            (. info setContent
                               (str name " @<br>" address))))))

(defn mark-map
  "Mark gmap for user with id."
  [gmap id user]
  (let [name (:name user)
        icon (if (= id (:id @state))
               (icon/get-icon-for-me)
               (icon/get-icon-for name))
        mark (new google.maps.Marker
                  (clj->js {:title name
                            :icon icon
                            :position (util/glatlng user)
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
        (.setPosition (util/glatlng user))
        (.setTitle name)))
    (let [mark (mark-map @my-map id user)]
      (swap! marks (fn [m] (assoc m id mark)))
      mark)))

(defn update-user-marks
  "Update user marks on map."
  []
  (request-update
   (fn [response]
     (util/log {:update-user-marks (count (:users response))})
     (doseq [[id user] (:users response)] (update-user id user)))))

(defn bound-marks
  "Pan gmap to show all marks."
  [gmap marks]
  (let [bounds (new google.maps.LatLngBounds)]
    (doseq [position (map (fn [m] (. m getPosition)) (vals marks))]
      (. bounds extend position))
    (. gmap fitBounds bounds)))

(defn add-marauder-controls
  "Add the marauder-control div to the RIGHT-BOTTOM of gmap."
  []
  (let [controls (. @my-map -controls)
        rb-corner (aget controls google.maps.ControlPosition.RIGHT-BOTTOM)
        buttons  (util/by-dom-id :marauder-buttons)
        search   (util/by-dom-id :marauder-search)
        place    (util/by-dom-id :marauder-place)
        everyone (util/by-dom-id :marauder-everyone)
        whereami (util/by-dom-id :marauder-whereami)
        box (new google.maps.places.SearchBox search)]
    (util/add-listener whereami "click"
                       #(. @my-map setCenter (util/glatlng @state)))
    (util/add-listener everyone "click"
                       #(bound-marks @my-map @marks))
    (util/add-listener place "click"
                       (fn []
                         (set! (.. search -style -display)
                               (get {"none" "inline-block"}
                                    (.. search -style -display) "none"))
                         (. search focus)))
    (util/add-listener box "places_changed"
                       (fn []
                         (. box setBounds (. @my-map getBounds))
                         (doseq [p (. box getPlaces)] (util/log p))
                         (set! (.. search -style -display) "none")))
    (. rb-corner push buttons)))

(defn initialize
  "Open a ROADMAP with everyone marked."
  []
  (request-update
   (fn [response]
     (util/log {:initialize (count (:users response))})
     (util/log (pr-str response))
     (remember! :id (first (keys (:you response))))
     (swap! my-map #(make-google-map (bound-response response)))
     (util/add-listener-once @my-map :idle
                             #(util/periodically update-user-marks 60000))
     (letfn [(mark [[id user]] [id (mark-map @my-map id user)])]
       (swap! marks (fn [m] (into m (map mark (:users response))))))
     (add-marauder-controls))))

(util/add-listener js/window :load initialize)
