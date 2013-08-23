(ns marauder.site
  (:require [clojure.string :as s]))

(defn query-string [kvs]
  "Encode the sequence of key-value pairs kvs into an HTTP query string."
  (s/join "&" (for [[k v] kvs]
                (str (name k) "=" (if (keyword? v) (name v) v)) )))

(defn url-qr-img
  "Return a Google Chart URL for a QR encoding url."
  [url]
  (str "http://chart.googleapis.com/chart?"
       (query-string {:cht :qr :chld :H :chs "200x200" :chl url})))

(def ^{:doc "Inline some CSS before full-time style tweaking."}
  css-inline
  (s/join "\n"
          ["html { height: 100%; }"
           "body { height: 100%; }"
           "#googlemapcanvas { height: 100%; margin: 0; padding: 0; }"
           "#marauder-back { margin: 5px; }"
           "#marauder-buttons { text-align: right; margin: 5px; }"
           "#marauder-find { margin-bottom: 5px; }"
           "#marauder-whereami { margin-top: 5px; }"
           "#marauder-me { margin: 5px; }"
           ".marauder-input { margin: 5px; width: 300px; }"
           ".marauder-icon { vertical-align: middle; }"
           "#marauder-join { width: 100%; }"
           ".marauder-center { width: 50%; text-align: center;; }"]))

(defn merge-nested
  "Merge a sequence of nested maps into a single nested map.
  Last wins as in merge and merge-with."
  [& maps]
  (if (every? map? maps)
    (apply merge-with merge-nested maps)
    (last maps)))
