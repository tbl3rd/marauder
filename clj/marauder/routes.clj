(ns marauder.routes
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [marauder.page :as page]
            [marauder.state :as state]))

(defroutes routes
  (GET "/" [] (page/page))
  (GET "/name/:id/:name" [id name]
       (state/name-user id name))
  (GET "/user/:id/:lat/:lng" [id lat lng]
       (state/update-user id lat lng))
  (route/resources "/")
  (route/not-found "Page not found"))

(def marauder-ring-app (handler/site routes))
