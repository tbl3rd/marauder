(ns marauder.mark
  (:require [marauder.icon :as icon]
            [marauder.state :as state]
            [marauder.util :as util]))

(def ^{:doc "The google.maps.Marker objects indexed by id."}
  marks (atom {}))

;; Add an Marker.info-window property.
;;
(defprotocol IHasInfoWindow
  (close-info [this])
  (open-info [this]))
(extend-type google.maps.Marker
  IHasInfoWindow
  (close-info [this]
    (if-let [w (. this -info-window)]
      (. w close)))
  (open-info [this]
    (let [title (. this getTitle)
          info (or (. this -info-window)
                   (set! (. this -info-window)
                         (new google.maps.InfoWindow
                              (js-obj "content" title))))]
      (util/after #(close-info this) 30000)
      (. info open (. this getMap) this)
      (util/raise info)
      (util/reverse-geocode
       this
       (fn [address] (. info setContent (str title " @<br>" address)))))))

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
              (if (= id (:id @state/state))
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
