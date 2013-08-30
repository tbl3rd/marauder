(ns marauder.map
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.controls :as controls]
            [marauder.mark :as mark]
            [marauder.state :as state]
            [marauder.util :as util]))

(def ^{:doc "A google.maps.Map that gets set once in initialize."}
  my-map (atom nil))

(defn bound-response
  "Get the LatLngBounds of all the positions in the update response."
  [response]
  (let [bounds (new google.maps.LatLngBounds)
        places (vals response)]
    (doseq [position (map util/glatlng places)] (. bounds extend position))
    bounds))

(defn request-update
  "Request an update from the server and (handle response)."
  [handle]
  (let [uuid (. (util/by-dom-id :marauder-uuid) -innerHTML)]
    (-> js/navigator
        (. -geolocation)
        (. getCurrentPosition
           (fn [position]
             (util/post (str "/update/" uuid)
                        (select-keys (swap! state/state
                                            merge (util/mlatlng position))
                                     [:id :name :lat :lng])
                        handle))))))

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
  (if-let [mark (get @mark/marks id)]
    (let [name (:name user)]
      (doto mark
        (.setPosition (util/glatlng user))
        (.setTitle name)))
    (let [mark (mark/mark-user @my-map id user)]
      (swap! mark/marks (fn [m] (assoc m id mark)))
      mark)))

(defn update-user-marks
  "Update user marks on map."
  []
  (request-update
   (fn [response]
     (let [rwoyou (dissoc response :you)]
      (util/log {:update-user-marks (count rwoyou)})
      (doseq [[id user] rwoyou] (update-user id user))))))

(defn initialize
  "Open a ROADMAP with everyone marked."
  []
  (request-update
   (fn [response]
     (util/log {:initialize (count (:users response))})
     (util/log (pr-str response))
     (state/set-my-id! (first (keys (:you response))))
     (let [rwoyou (dissoc response :you)
           gmap (swap! my-map #(make-google-map (bound-response rwoyou)))]
      (util/add-listener-once gmap :idle
                              #(util/periodically update-user-marks 60000))
      (letfn [(mark [[id user]] [id (mark/mark-user gmap id user)])]
        (swap! mark/marks (fn [m] (into m (map mark rwoyou)))))
      (controls/add-all-controls gmap)))))

(util/add-listener js/window :load initialize)
