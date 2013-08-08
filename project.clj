(defproject marauder "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.6"]
                 [com.cemerick/piggieback "0.1.0"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.8.6"]]
  :source-paths ["clj"]
  :ring {:handler marauder.routes/marauder-ring-app}
  :hooks [leiningen.cljsbuild]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :cljsbuild
  {:builds [{:source-paths ["cljs"]
             :compiler {:output-to "resources/public/js/marauder.js"
                        :optimizations :whitespace
                        :pretty-print true}
             :jar true}]})
