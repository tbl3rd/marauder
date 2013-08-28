(ns marauder.mark
  (:require [marauder.icon :as icon]
            [marauder.state :as state]
            [marauder.util :as util]))

(def ^{:doc "The google.maps.Marker objects indexed by id."}
  marks (atom {}))

(defn info-content
  "Add content to the info window of mark."
  [mark]
  (let [info (. mark -info-window)
        title (. mark getTitle)]
    (. info setContent title)
    (util/reverse-geocode
     mark
     (fn [address] (. info setContent (str title " @<br>" address))))))

(defn open-info
  "Open an info window on mark displaying a name and address."
  [mark]
  (let [title (. mark getTitle)
        info (or (. mark -info-window)
                 (set! (. mark -info-window)
                       (new google.maps.InfoWindow)))]
    (info-content mark)
    (util/after #(. info close) 30000)
    (. info open (. mark getMap) mark)
    (util/raise info)))

(defn new-mark
  "New marker at position on gmap with icon and title."
  [gmap position icon title]
  (let [mark (new google.maps.Marker
                  (clj->js {:map gmap
                            :position position
                            :icon icon
                            :title title
                            :animation google.maps.Animation.DROP}))]
    (util/add-listener mark "click" #(open-info mark))
    mark))

(defn mark-place
  "Mark gmap for place."
  [gmap place]
  (new-mark gmap
            (.. place -geometry -location)
            (icon/get-icon-for-place place)
            (or (. place -name) "Here")))

(defn mark-user
  "Mark gmap for user with id."
  [gmap id user]
  (let [user-name (:name user)]
    (util/log {"mark-user" user-name})
    (new-mark gmap
              (util/glatlng user)
              (if (= id (state/get-my-id))
                (icon/get-icon-for-me)
                (icon/get-icon-for user-name))
              user-name)))

(defn bound-marks
  "Pan gmap to show all marks."
  [gmap]
  (let [bounds (new google.maps.LatLngBounds)]
    (doseq [position (map (fn [m] (. m getPosition)) (vals @marks))]
      (. bounds extend position))
    (. gmap fitBounds bounds)))

(defn name-my-mark
  "Put name on my mark."
  [name]
  (state/set-my-name! name)
  (let [my-mark (get @marks (state/get-my-id))]
    (. my-mark setTitle name)
    (info-content my-mark)))

