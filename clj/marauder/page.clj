(ns marauder.page
  (:require [clojure.string :as s]
            [hiccup.element :refer [javascript-tag]]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn- make-query-string [map]
  (s/join "&" (for [[k v] map] (str (name k) "=" v) )))

(defn- google-maps-api-key []
  "AIzaSyA10Vv0gV5yLp1WcTpqoA9hhILt_Rhc6OQ")

(defn- google-maps-url [key sensor?]
  (str "http://maps.googleapis.com/maps/api/js?"
       (make-query-string {:libraries "places" :sensor sensor? :key key})))

(def googlemapcanvas-css-inline
  (s/join "\n"
          ["html { height: 100%; }"
           "body { height: 100%; }"
           "#googlemapcanvas { height: 100%; margin: 0; padding: 0; }"
           "#marauder-buttons { text-align: right; margin: 5px; width: 50%; }"
           "#marauder-search { display: none; margin: 5px; width: 50%; }"
           "#marauder-place { vertical-align: middle; }"]))

(defn- layout [& content]
  (html5
   [:head
    [:title "Marauder"]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "initial-scale=1.0, user-scalable=no"}]
    [:style {:type "text/css"} googlemapcanvas-css-inline]
    (include-css "css/marauder.css")
    (include-js (google-maps-url (google-maps-api-key) false))]
   [:body content]))

(defn- marauder-buttons
  []
  [:div#marauder-buttons
   [:div#marauder-whereami {:title "Where am I?"}
    [:img {:src "img/whereami.png" :alt "whereami"}]]
   [:div#marauder-everyone {:title "Where is everyone?"}
    [:img {:src "img/marker.png" :alt "everyone"}]]
   [:span
    [:input#marauder-search {:type "text"
                             :placeholder "Search for ..."}]
    [:img#marauder-place {:title "Right here!"
                          :src "img/dd-start.png"
                          :alt "place"}]]])

(defn page []
  (layout
   [:div#googlemapcanvas]
   (marauder-buttons)
   (include-js "js/marauder.js")
   (comment [:script {:type "text/javascript"} "alert('marauder loaded')"])))
