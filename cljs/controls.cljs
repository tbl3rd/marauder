(ns marauder.controls
  (:require [marauder.mark :as mark]
            [marauder.state :as state]
            [marauder.util :as util]))

(defn set-input-value
  "Set the value property of input element to value."
  [input value]
  (goog.dom.setProperties input (js-obj "value" value)))

(defn toggle-input
  "Toggle input element between display style none and inline-block."
  [input]
  (set! (.. input -style -display)
        (get {"none" "inline-block"}
             (.. input -style -display) "none")))

(defn add-place-search-control
  "Add the search for a place control to gmap."
  [gmap]
  (let [search (util/by-dom-id :marauder-search)
        place (util/by-dom-id :marauder-place)
        search-box (new google.maps.places.SearchBox search)]
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
                         (. search-box setBounds (. gmap getBounds))
                         (if-let [place (first (. search-box getPlaces))]
                           (mark/mark-place gmap place))
                         (set! (.. search -style -display) "none")))))

(defn add-everyone-control
  "Add the where is everyone control to gmap."
  [gmap]
  (let [qr-code (util/by-dom-id :marauder-qr-code)
        everyone (util/by-dom-id :marauder-everyone)]
    (toggle-input qr-code)
    (util/log {:everyone everyone})
    (util/add-listener everyone "click"
                       (fn []
                         (toggle-input qr-code)
                         (set-input-value qr-code (. qr-code -placeholder))
                         (. qr-code focus)
                         (. qr-code select)
                         (mark/bound-marks gmap)))
    (util/log {:qr-code qr-code})))

(defn add-whereami-control
  "Add the where am I control to gmap."
  [gmap]
  (let [name-me (util/by-dom-id :marauder-me)
        whereami (util/by-dom-id :marauder-whereami)]
    (toggle-input name-me)
    (util/log {:whereami whereami})
    (util/add-listener whereami "click"
                       (fn []
                         (. gmap setCenter (util/glatlng @state/state))
                         (toggle-input name-me)
                         (set-input-value name-me "")
                         (. name-me focus)))
    (util/log {:name-me name-me})
    (util/add-listener name-me "change"
                       (fn []
                         (state/remember! :name (. name-me -value))
                         (set! (.. name-me -style -display) "none")))))

(defn add-location-controls
  "Add the location controls to the right-bottom corner of gmap"
  [gmap controls]
  (let [buttons (util/by-dom-id :marauder-buttons)
        rb-corner (aget controls google.maps.ControlPosition.RIGHT-BOTTOM)]
    (add-place-search-control gmap)
    (add-everyone-control gmap)
    (add-whereami-control gmap)
    (. rb-corner push buttons)))

(defn add-back-to-qr-control
  "Add the back control to the right-top."
  [controls]
  (let [back (util/by-dom-id :marauder-back)
        rt-corner (aget controls google.maps.ControlPosition.RIGHT-TOP)]
    (util/log {:back back})
    (. rt-corner push back)))

(defn add-all-controls
  "Add the all the custom controls to gmap."
  [gmap]
  (let [controls (. gmap -controls)]
    (add-back-to-qr-control controls)
    (add-location-controls gmap controls)))
