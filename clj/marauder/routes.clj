(ns marauder.routes
  (:require [clojure.string :as s]
            [clojure.edn]
            [compojure.core :refer [defroutes ANY GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.request :as request]
            [marauder.map :as map]
            [marauder.qr :as qr]
            [marauder.state :as state]))

(defn edn-response [data & [status]]
  "An HTTP response of EDN data with status."
  {:status (or status 200)
   :headers {"Content-type" "application/edn"}
   :body (pr-str data)})

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
  (fn [request] (println request))
  (ANY "/echo" request
        (pr-str "ECHO" request))
  (POST "/update" request
        (edn-response (state/update-user (:marauder-edn request))))
  (GET "/" request
       (let [uuid (str (java.util.UUID/randomUUID))
             url (str (request/request-url request) "map/" uuid)]
         (qr/qr-page url uuid)))
  (GET "/join/:uuid" [uuid :as request]
       (qr/qr-page (request/request-url request) uuid))
  (GET "/map/:uuid" [uuid :as request]
       (map/map-page (request/request-url request) uuid))
  (route/resources "/")
  (route/resources "/join")
  (route/resources "/map")
  (route/not-found "This is not the page you are looking for."))

(def marauder-ring-app
  (-> (handler/site routes)
      (wrap-marauder-edn)))
