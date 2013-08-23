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
                   (pr-str {:id nil :name "Ishmael"})))))

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
  ([gmap mark address]
     (let [name (. mark getTitle)
           info (new google.maps.InfoWindow (clj->js {:content name}))]
       (util/raise info)
       (. info open gmap mark)
       (. info setContent (str name " @<br>" address))
       (util/after #(. info close) 30000)
       info))
  ([gmap mark]
     (util/reverse-geocode
      mark
      (fn [address] (open-info gmap mark address)))))

(defn mark-place
  "Mark gmap for place."
  [gmap place]
  (let [address (. place -formatted-address)
        name (or (. place -name) "Here")
        icon (icon/get-icon-for-place place)
        mark (util/new-marker gmap (.. place -geometry -location) icon name)]
    (util/add-listener mark "click" #(open-info gmap mark address))
    mark))

(defn mark-user
  "Mark gmap for user with id."
  [gmap id user]
  (let [name (:name user)
        icon (if (= id (:id @state))
               (icon/get-icon-for-me)
               (icon/get-icon-for name))
        mark (util/new-marker gmap (util/glatlng user) icon name)]
    (util/add-listener mark "click" #(open-info gmap mark))
    mark))

(defn update-user
  "Update marks for user with id.  Return the user's mark."
  [id user]
  (if-let [mark (get @marks id)]
    (let [name (:name user)]
      (doto mark
        (.setPosition (util/glatlng user))
        (.setTitle name)))
    (let [mark (mark-user @my-map id user)]
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
        rt-corner (aget controls google.maps.ControlPosition.RIGHT-TOP)
        rb-corner (aget controls google.maps.ControlPosition.RIGHT-BOTTOM)
        back     (util/by-dom-id :marauder-back)
        buttons  (util/by-dom-id :marauder-buttons)
        search   (util/by-dom-id :marauder-search)
        place    (util/by-dom-id :marauder-place)
        qr-code  (util/by-dom-id :marauder-qr-code)
        everyone (util/by-dom-id :marauder-everyone)
        name-me  (util/by-dom-id :marauder-me)
        whereami (util/by-dom-id :marauder-whereami)
        search-box (new google.maps.places.SearchBox search)
        toggle-input (fn [input]
                       (set! (.. input -style -display)
                             (get {"none" "inline-block"}
                                  (.. input -style -display) "none")))
        set-input-value (fn [input value]
                          (goog.dom.setProperties input
                                                  (js-obj "value" value)))]

    (toggle-input search)
    (util/log {:place place})
    (util/add-listener place "click"
                       (fn []
                         (toggle-input search)
                         (set-input-value search "")
                         (. search focus)))
    (util/log {:search-box search-box})
    (util/add-listener search-box "places_changed"
                       (fn []
                         (. search-box setBounds (. @my-map getBounds))
                         (if-let [place (first (. search-box getPlaces))]
                           (mark-place @my-map place))
                         (set! (.. search -style -display) "none")))

    (toggle-input qr-code)
    (util/log {:everyone everyone})
    (util/add-listener everyone "click"
                       (fn []
                         (toggle-input qr-code)
                         (set-input-value qr-code (. qr-code -placeholder))
                         (. qr-code focus)
                         (. qr-code select)
                         (bound-marks @my-map @marks)))
    (util/log {:qr-code qr-code})

    (toggle-input name-me)
    (util/log {:whereami whereami})
    (util/add-listener whereami "click"
                       (fn []
                         (. @my-map setCenter (util/glatlng @state))
                         (toggle-input name-me)
                         (set-input-value name-me "")
                         (. name-me focus)))
    (util/log {:name-me name-me})
    (util/add-listener name-me "change"
                       (fn []
                         (remember! :name (. name-me -value))
                         (set! (.. name-me -style -display) "none")))
    (util/log {:back back})
    (. rt-corner push back)
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
     (letfn [(mark [[id user]] [id (mark-user @my-map id user)])]
       (swap! marks (fn [m] (into m (map mark (:users response))))))
     (add-marauder-controls))))

(util/add-listener js/window :load initialize)
