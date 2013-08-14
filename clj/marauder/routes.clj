(ns marauder.routes
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.params :refer [wrap-params]]
            [marauder.page :as page]
            [marauder.state :as state]))

(defn edn-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-type" "application/edn"}
   :body (pr-str data)})

(defroutes routes
  (GET "/name/:id/:name" [id name]
       (state/name-user id name))
  (GET "/user/:id/:lat/:lng" [id lat lng]
       (state/update-user id lat lng))
  (POST "/echo" request
        (pr-str "ECHO" {:status 200
                        :body  request}))
  (GET "/" [] (page/page))
  (route/resources "/")
  (route/not-found "This is not the page you are looking for."))

(def marauder-ring-app
  (-> (handler/site routes)
      (wrap-restful-format)))

(def minimal-ring-request-map {:server-port 80
                               :server-name "127.0.0.1"
                               :remote-addr "127.0.0.1"
                               :uri "/"
                               :scheme :http
                               :headers {}
                               :request-method :get})
