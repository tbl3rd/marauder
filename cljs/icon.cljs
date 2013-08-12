(ns marauder.icon)

(def index (atom 0))

(def icons ["img/mm_20_blue.png"
            "img/mm_20_green.png"
            "img/mm_20_orange.png"
            "img/mm_20_purple.png"
            "img/mm_20_yellow.png"
            "img/mm_20_red.png"
            "img/mm_20_white.png"
            "img/mm_20_gray.png"
            "img/mm_20_black.png"
            "img/mm_20_brown.png"])

(defn next-index [n]
  (let [next (+ 1 n)]
    (if (< next (count icons)) next 0)))

(defn get-icon []
  (let [result (get icons @index)]
    (swap! index next-index)
    result))
