(ns marauder.state
  (:require [cljs.reader :as reader-but-who-cares?]
            [marauder.util :as util]))

(def ^{:doc "Local user state cached across sessions."}
  state (atom (cljs.reader/read-string
               (or (. js/localStorage getItem "state")
                   (pr-str {:id nil :name "Ishmael"})))))

(defn remember!
  "Remember that key k has value v across sessions."
  [k v]
  (util/log {k v})
  (let [value (swap! state assoc k v)]
    (. js/localStorage setItem "state" (pr-str value))))
