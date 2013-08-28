(ns marauder.map
  (:require
   [clojure.string :as s]
   [hiccup.page :refer [html5 include-css include-js]]
   [marauder.site :refer [css-inline query-string url-qr-img]]))

(defn- google-maps-api-key
  "Just what it says."
  []
  "AIzaSyA10Vv0gV5yLp1WcTpqoA9hhILt_Rhc6OQ")

(defn- google-maps-url
  "The API URL for Google Maps with key and sensor flag in query."
  [key sensor?]
  (str "http://maps.googleapis.com/maps/api/js?"
       (query-string {:libraries "places" :sensor sensor? :key key})))

(defn- back-to-qr
  "A return to this map's QR code page control."
  [map-url]
  [:div#marauder-back
   [:a {:href (s/replace map-url "/map/" "/qr/")}
    [:img.marauder-icon {:title "Back to QR."
                         :src "img/qr.png"
                         :alt "QR"}]]])

(defn- marauder-find
  "Control for dropping new place markers on the map."
  []
  [:div#marauder-find.marauder-field
   [:span
    [:input#marauder-search.marauder-input {:title "Where to?"
                                            :type "text"
                                            :placeholder "Where is ... ?"}]
    [:img#marauder-place.marauder-icon {:title "Where is?"
                                        :src "img/dd-start.png"
                                        :alt "place"}]]])

(defn- marauder-everyone
  "Control for bringing all markers onto map."
  []
  [:div#marauder-everyone.marauder-field
   [:span
    [:img.marauder-icon {:title "Where is everyone?"
                         :src "img/marker.png"
                         :alt "everyone"}]]])

(defn- marauder-whereami
  "Control for naming and centering the map on me."
  []
  [:div#marauder-whereami.marauder-field
   [:span
    [:input#marauder-me.marauder-input {:title "Give yourself a name."
                                        :type "text"
                                        :placeholder "Call me Ishmael?"}]
    [:img.marauder-icon {:title "Where am I?"
                         :src "img/whereami.png"
                         :alt "whereami"}]]])

(defn- marauder-buttons
  "The custom controls on the map page for uuid."
  []
  [:div#marauder-buttons
   (marauder-find)
   (marauder-everyone)
   (marauder-whereami)])

(defn map-page
  "The map page for uuid."
  [map-url uuid]
  (html5
   [:head
    [:title (str "Marauder Map (" uuid ")")]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "initial-scale=1.0, user-scalable=no"}]
    [:style {:type "text/css"} css-inline]
    (include-css "css/marauder.css")
    (include-js (google-maps-url (google-maps-api-key) false))]
   [:body
    [:div#googlemapcanvas]
    (back-to-qr map-url)
    (marauder-buttons)
    (include-js "js/marauder.js")
    (comment [:script {:type "text/javascript"} "alert('marauder loaded')"])]))
