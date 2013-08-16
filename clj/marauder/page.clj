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
       (make-query-string {:key key :sensor sensor?})))

(def google-maps-viewport-css-inline
  (s/join "\n"
          ["html { height: 100%; }"
           "body { height: 100%; }"
           "#map-canvas { height: 100%; margin: 0; padding: 0; }"]))

(defn- layout [& content]
  (html5
   [:head
    [:title "Marauder"]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "initial-scale=1.0, user-scalable=no"}]
    [:style {:type "text/css"} google-maps-viewport-css-inline]
    (include-css "css/marauder.css")
    (include-js (google-maps-url (google-maps-api-key) false))]
   [:body content]))

(defn page []
  (layout
   [:div#map-canvas]
   (include-js "js/marauder.js")
   (comment [:script {:type "text/javascript"} "alert('marauder loaded')"])))
