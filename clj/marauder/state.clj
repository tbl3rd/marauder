(ns marauder.state
  (:require [marauder.site :refer [merge-nested]]))

(defn initial-state []
  {"618cf96b-9289-47f8-a8b2-d228fc22d490"
   {1 {:time 0 :lat 42.369706 :lng -71.060257 :kind :place :name "Boston"   }
    2 {:time 0 :lat 42.378836 :lng -71.110436 :kind :place :name "Cambridge"}
    3 {:time 0 :lat 42.382545 :lng -71.137122 :kind :place :name "home"     }
    4 {:time 0 :lat 42.366931 :lng -71.091352 :kind :place :name "work"     }}})

(def ^{:doc
       "A {map-id {user-id {k v ...}} ...} mapping map IDs to maps of
        place IDs to place info maps."}
  state
  (atom (initial-state)))

(def next-id (atom 9))
(defn next-id-for-debugging
  "Return a new integer ID."
  []
  (let [result @next-id]
    (swap! next-id inc)))

(defn update-user
  "Update state with user position in update."
  [uuid update]
  (let [id (or (:id update) (next-id-for-debugging))
        uwoid (dissoc update :id)
        merger (fn [s] (merge-nested s {uuid {id uwoid}}))
        new-state (swap! state merger)]
    (assoc (get new-state uuid) :you {id uwoid})))
