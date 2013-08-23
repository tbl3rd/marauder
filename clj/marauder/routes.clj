(ns marauder.routes
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [clojure.edn]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [marauder.page :as page]))

(defn initial-state []
  {:users
   {0 {:name "Boston"    :lat 42.369706 :lng -71.060257}
    1 {:name "Cambridge" :lat 42.378836 :lng -71.110436}
    2 {:name "home"      :lat 42.382545 :lng -71.137122}
    3 {:name "work"      :lat 42.366931 :lng -71.091352}}
   :places
   {}})

(def ^{:doc "Everything tracked."}
  state
  (atom (initial-state)))

(defn edn-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-type" "application/edn"}
   :body (pr-str data)})

(defn merge-nested
  "Merge a sequence of nested maps into a single nested map.
  Last wins as in merge and merge-with."
  [& maps]
  (if (every? map? maps)
    (apply merge-with merge-nested maps)
    (last maps)))

(defn new-untagged-uuid [] (str (java.util.UUID/randomUUID)))
(def next-id (atom (count (:users @state))))
(defn next-id-for-debugging
  "Return a new integer ID."
  []
  (let [result @next-id]
    (swap! next-id inc)))

(defn update-user
  "Update state with user position in update."
  [update]
  (println "update-user" update)
  (let [id (or (:id update) (next-id-for-debugging))
        uwoid (dissoc update :id)
        merger (fn [s] (merge-nested s {:users {id uwoid}}))
        new-state (swap! state merger)]
    (assoc new-state :you {id uwoid})))

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
        (edn-response (update-user (:marauder-edn request))))
  (POST "/echo" request
        (pr-str "ECHO" request))
  (GET "/" []
       (page/join-page (str (java.util.UUID/randomUUID))))
  (GET "/join/:uuid" [uuid]
       (page/join-page uuid))
  (GET "/map/:uuid" [uuid]
       (page/map-page uuid))
  (route/resources "/")
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
