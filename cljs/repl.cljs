(ns marauder.brepl
  (:require [clojure.browser.repl :as brepl]))

(brepl/connect "http://localhost:4321/repl")
