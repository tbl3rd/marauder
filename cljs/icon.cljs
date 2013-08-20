(ns marauder.icon
  (:require [clojure.string :as string-but-who-cares]))

(def ^{:doc "Some small colored icons for get-some-icon."
       :private true}
  small-colored-icons ["img/mm_20_blue.png"
                       "img/mm_20_green.png"
                       "img/mm_20_orange.png"
                       "img/mm_20_purple.png"
                       "img/mm_20_yellow.png"
                       "img/mm_20_red.png"
                       "img/mm_20_white.png"
                       "img/mm_20_gray.png"
                       "img/mm_20_black.png"
                       "img/mm_20_brown.png"])

(def ^{:doc "An index into small-colored-icons for get-some-icon."
       :private true}
  index (atom 0))

(defn- next-index
  "Return the next index into small-colored-icons for get-some-icon."
  [n]
  (let [next (+ 1 n)]
    (if (< next (count small-colored-icons)) next 0)))

(defn get-some-icon
  "Return some small colored icon."
  []
  (let [result (get small-colored-icons @index)]
    (swap! index next-index)
    result))

(defn get-icon-for
  "Return an icon initialed for name."
  [name]
  (str "img/marker" (clojure.string/upper-case (first name)) ".png"))

(defn get-icon-for-me
  "Return the you-are-here icon to mark the owner of the client."
  []
  "img/arrow-green.png"
  "img/whereami.png")
