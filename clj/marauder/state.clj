(ns marauder.state)

(defn initial-state []
  {:users
   {"6" {:name "Boston"    :lat 42.369706 :lng -71.060257}
    "7" {:name "Cambridge" :lat 42.378836 :lng -71.110436}
    "8" {:name "home"      :lat 42.382545 :lng -71.137122}
    "9" {:name "work"      :lat 42.366931 :lng -71.091352}}
   :places
   {}})

(def ^{:doc "Everything tracked."}
  state
  (atom (initial-state)))

(defn update-user-state
  [s id lat lng]
  (let [user (get s id {:name "anonymous"})
        new-user (-> user
                     (assoc :lat lat)
                     (assoc :lng lng))]
    (assoc s id new-user)))

(defn update-user
  [id lat lng]
  (swap! state update-user-state id lat lng))

(defn name-user-state
  [s id name]
  (let [user (get s id {})
        new-user (-> user
                     (assoc :name name))]
    (assoc s id new-user)))

(defn name-user
  [id name]
  (swap! state update-user-state id name))
