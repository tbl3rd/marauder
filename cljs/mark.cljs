(ns marauder.mark
  (:require [marauder.icon :as icon]
            [marauder.state :as state]
            [marauder.util :as util]))

(def ^{:doc "The google.maps.Marker objects indexed by id."}
  marks (atom {}))

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
        icon (if (= id (:id @state/state))
               (icon/get-icon-for-me)
               (icon/get-icon-for name))
        mark (util/new-marker gmap (util/glatlng user) icon name)]
    (util/add-listener mark "click" #(open-info gmap mark))
    mark))

(defn bound-marks
  "Pan gmap to show all marks."
  [gmap marks]
  (let [bounds (new google.maps.LatLngBounds)]
    (doseq [position (map (fn [m] (. m getPosition)) (vals marks))]
      (. bounds extend position))
    (. gmap fitBounds bounds)))
