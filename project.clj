(defproject marauder "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1586"]
                 [clj-http "0.7.6"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]
                 [ring "1.2.0"]
                 [ring-middleware-format "0.3.0"]
                 [ring-server "0.2.8"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.6"]]
  :source-paths ["clj"]
  :ring {:auto-refresh? true
         :auto-reload? true
         :handler marauder.routes/marauder-ring-app}
  :cljsbuild
  {:repl-listen-port 9000
   :builds [{:source-paths ["cljs"]
             :compiler {:output-to "resources/public/js/marauder.js"
                        :optimizations :whitespace
                        :pretty-print true}
             :jar true}]})

;; Start ring server with: lein ring server-headless
;; Start ClojureScript browser repl with: ~/bin/browser-repl :port 4321
