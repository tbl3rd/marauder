(ns marauder.page
  (:require [clojure.string :as s]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn- make-query-string [kvs]
  (s/join "&" (for [[k v] kvs]
                (str (name k) "=" (if (keyword? v) (name v) v)) )))

(defn- google-maps-api-key []
  "AIzaSyA10Vv0gV5yLp1WcTpqoA9hhILt_Rhc6OQ")

(defn- google-maps-url [key sensor?]
  (str "http://maps.googleapis.com/maps/api/js?"
       (make-query-string {:libraries "places" :sensor sensor? :key key})))

(def ^{:doc "Inline some CSS before full-time style tweaking."
       :private true}
  marauder-css-inline
  (s/join "\n"
          ["html { height: 100%; }"
           "body { height: 100%; }"
           "#googlemapcanvas { height: 100%; margin: 0; padding: 0; }"
           "#marauder-buttons { text-align: right; margin: 5px; }"
           "#marauder-find { margin-bottom: 5px; }"
           "#marauder-search { margin: 5px; width: 300px; }"
           "#marauder-place { vertical-align: middle; }"]))

(defn new-map-url-qr-img
  "Return a Google Chart URL for a QR encoding url."
  [url]
  (str "http://chart.googleapis.com/chart?"
       (make-query-string {:cht :qr :chld :H :chs "200x200" :chl url})))

(new-map-url-qr-img (str (java.util.UUID/randomUUID)))

(defn- marauder-buttons
  "The custom controls on the map page."
  []
  [:div#marauder-buttons
   [:div#marauder-find
    [:span
     [:input#marauder-search {:title "Where to?"
                              :type "text"
                              :placeholder "Where is ... ?"}]
     [:img#marauder-place {:title "Where is?"
                           :src "img/dd-start.png"
                           :alt "place"}]]]
   [:div#marauder-everyone
    [:img {:title "Where is everyone?"
           :src "img/marker.png"
           :alt "everyone"}]]
   [:div#marauder-whereami {}
    [:img {:title "Where am I?"
           :src "img/whereami.png"
           :alt "whereami"}]]])

(defn map-page
  "The map page for uuid."
  [uuid]
  (println "map-page " uuid)
  (html5
   [:head
    [:title (str "Marauder Map (" uuid ")")]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "initial-scale=1.0, user-scalable=no"}]
    [:style {:type "text/css"} marauder-css-inline]
    (include-css "css/marauder.css")
    (include-js (google-maps-url (google-maps-api-key) false))]
   [:body
    [:div#googlemapcanvas]
    (marauder-buttons)
    (include-js "js/marauder.js")
    (comment [:script {:type "text/javascript"} "alert('marauder loaded')"])]))

(defn join-page
  "Show a new map URL with a new UUID."
  []
  (let [uuid (str (java.util.UUID/randomUUID))
        url (str "http://localhost:3000/map/" uuid)
        qr (new-map-url-qr-img url)]
      (html5
       [:head
        [:title (str "Marauder (" uuid ")")]
        [:meta {:charset "UTF-8"}]
        (include-css "css/marauder.css")]
       [:body
        [:div#hello "Take this map."]
        [:div#qr
         [:img {:title (str "URL: " url)
                :src qr
                :alt "The map URL"}]]
        [:div#goodbye "May it serve you well."]])))
