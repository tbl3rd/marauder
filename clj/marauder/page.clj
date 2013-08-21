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
           "#marauder-searchbox { display: none; width: 50%; }"
           "#marauder-where { width: 100%; }"]))

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

(defn- marauder-searchbox
  []
  [:div#marauder-searchbox
   [:input#marauder-where {:type "text" :placeholder "Search for ..."}]])

(defn- marauder-controls
  []
  [:div#marauder-controls
   [:div#marauder-whereami {:title "Where am I?"}
    [:img {:src "img/whereami.png" :alt "whereami"}]]
   [:div#marauder-everyone {:title "Where is everyone?"}
    [:img {:src "img/marker.png" :alt "everyone"}]]
   [:div#marauder-place {:title "Right here!"}
    [:img {:src "img/dd-start.png" :alt "place"}]]])

(defn page []
  (layout
   [:div#googlemapcanvas]
   (marauder-searchbox)
   (marauder-controls)
   (include-js "js/marauder.js")
   (comment [:script {:type "text/javascript"} "alert('marauder loaded')"])))
