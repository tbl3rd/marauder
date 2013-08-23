(ns marauder.state
  (:require [marauder.site :refer [merge-nested]]))

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
