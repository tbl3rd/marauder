(ns marauder.map
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.controls :as controls]
            [marauder.icon :as icon]
            [marauder.state :as state]
            [marauder.util :as util]))

(def ^{:doc "A google.maps.Map that gets set once in initialize."}
  my-map (atom nil))

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
                      (select-keys (swap! state/state
                                          merge (util/mlatlng position))
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

(defn update-user
  "Update marks for user with id.  Return the user's mark."
  [id user]
  (if-let [mark (get @state/marks id)]
    (let [name (:name user)]
      (doto mark
        (.setPosition (util/glatlng user))
        (.setTitle name)))
    (let [mark (mark-user @my-map id user)]
      (swap! state/marks (fn [m] (assoc m id mark)))
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

(defn initialize
  "Open a ROADMAP with everyone marked."
  []
  (request-update
   (fn [response]
     (util/log {:initialize (count (:users response))})
     (util/log (pr-str response))
     (state/remember! :id (first (keys (:you response))))
     (let [gmap (swap! my-map #(make-google-map (bound-response response)))]
      (util/add-listener-once gmap :idle
                              #(util/periodically update-user-marks 60000))
      (letfn [(mark [[id user]] [id (state/mark-user gmap id user)])]
        (swap! state/marks (fn [m] (into m (map mark (:users response))))))
      (controls/add-all-controls gmap)))))

(util/add-listener js/window :load initialize)
