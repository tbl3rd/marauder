(ns marauder.routes
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [marauder.page :as page]))

(defroutes routes
  (GET "/" [] (page/page))
  (route/resources "/")
  (route/not-found "Page not found"))

(def marauder-ring-app (handler/site routes))
