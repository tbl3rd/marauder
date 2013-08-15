(ns marauder.routes
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.params :refer [wrap-params]]
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
  []
  (let [result @next-id]
    (swap! next-id inc)))

(defn update-user
  [update]
  (let [id (or (:id update) (next-id-for-debugging))
        uwoid (dissoc update :id)
        merger (fn [s] (merge-nested s {:users {id uwoid}}))
        new-state (swap! state merger)]
    (assoc new-state :you {id uwoid})))

(defn wrap-log [app]
  (fn [request]
    (println (select-keys (:body request)
                          [:uri :params :body-params :form-params]))
    (app request)))

(defroutes routes
  (POST "/update" request
        (pr-str (update-user (:body-params request))))
  (POST "/echo" request
        (pr-str "ECHO" (:body-params request)))
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
