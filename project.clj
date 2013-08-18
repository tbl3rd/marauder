(defproject marauder "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1586"]
                 [org.clojure/tools.reader "0.7.6"]
                 [clj-http "0.7.6"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.6"]]
  :source-paths ["clj"]
  :ring {:auto-refresh? true
         :auto-reload? true
         :handler marauder.routes/marauder-ring-app}
  :cljsbuild
  {:repl-listen-port 8888
   :builds [{:source-paths ["cljs"]
             :compiler {:output-to "resources/public/js/marauder.js"
                        :optimizations :whitespace
                        :pretty-print true}
             :jar true}]})

;; Start ring server with: lein ring server-headless
;; Start ClojureScript updates with: lein cljsbuild auto
;; Start ClojureScript browser repl with: ~/bin/browser-repl :port 8888
