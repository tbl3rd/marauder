(ns marauder.routes
  (:require [clojure.string :as s]
            [clojure.edn]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [marauder.map :as map]
            [marauder.qr :as qr]
            [marauder.state :as state]))

(defn edn-response [data & [status]]
  "An HTTP response of EDN data with status."
  {:status (or status 200)
   :headers {"Content-type" "application/edn"}
   :body (pr-str data)})

(defn wrap-log
  "Log some keys from request to the server console."
  [app]
  (fn [request]
    (println (select-keys
              (:body request)
              [:uri :params :body-params :form-params]))
    (app request)))

(defn wrap-marauder-edn
  "Add to request a :marauder-edn key with value an edn reading of :body."
  [app]
  (fn [request]
    (let [body (:body request)
          read (fn [in] (clojure.edn/read {:eof nil} in))
          in (new java.io.PushbackReader (new java.io.InputStreamReader body))
          with-edn (assoc request :marauder-edn (read in))]
      (println :wrap-marauder-edn (:marauder-edn with-edn))
      (app with-edn))))

(defroutes routes
  (POST "/update" request
        (edn-response (state/update-user (:marauder-edn request))))
  (POST "/echo" request
        (pr-str "ECHO" request))
  (GET "/" []
       (qr/qr-page (str (java.util.UUID/randomUUID))))
  (GET "/join/:uuid" [uuid]
       (qr/qr-page uuid))
  (GET "/map/:uuid" [uuid]
       (map/map-page uuid))
  (route/resources "/")
  (route/resources "/join")
  (route/resources "/map")
  (route/not-found "This is not the page you are looking for."))

(def marauder-ring-app
  (-> (handler/site routes)
      (wrap-marauder-edn)))

(def minimal-ring-request-map {:server-port 80
                               :server-name "127.0.0.1"
                               :remote-addr "127.0.0.1"
                               :uri "/"
                               :scheme :http
                               :headers {}
                               :request-method :get})
