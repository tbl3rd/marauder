(ns marauder.qr
  (:require [clojure.string :as s]
            [hiccup.page :refer [html5 include-css]]
            [marauder.site :refer [css-inline query-string url-qr-img]]))

(defn qr-page
  "Show a new map URL with uuid."
  [join-url uuid]
  (let [url (s/replace join-url "/join/" "/map/")
        qr (url-qr-img url)]
    (html5
     [:head
      [:title (str "Marauder (" uuid ")")]
      [:meta {:charset "UTF-8"}]
      [:style {:type "text/css"} css-inline]
      (include-css "css/marauder.css")]
     [:body
      [:div#marauder-join
       [:div.marauder-center "Take this map."]
       [:div#marauder-qr.marauder-center
        [:a {:href url}
         [:img {:title (str "URL: " url) :src qr :alt qr}]]]
       [:div.marauder-center "May it serve you well."]]])))
