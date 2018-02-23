(defproject slacky "0.1.0-SNAPSHOT"
  :description "Memes-as-a-Service"
  :url "https://github.com/oliyh/slacky"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [pedestal-api "0.3.1"]
                 [io.pedestal/pedestal.service "0.5.2"]
                 [io.pedestal/pedestal.jetty "0.5.2"]
                 [angel-interceptor "0.3.0"]

                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]

                 [org.clojure/tools.logging "0.4.0"]
                 [clj-http "3.7.0"]
                 [cheshire "5.8.0"]
                 [org.clojure/core.memoize "0.5.9"]

                 ;; web
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]

                 ;; persistence
                 [org.clojure/java.jdbc "0.7.1"]
                 [org.postgresql/postgresql "42.1.4"]
                 [com.h2database/h2 "1.4.196"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [joplin.core "0.3.10"]
                 [joplin.jdbc "0.3.10"]
                 [honeysql "0.9.1"]]
  :main ^:skip-aot slacky.server
  :min-lein-version "2.0.0"
  :target-path "target/%s"
  :resource-paths ["config", "resources", "migrators"]
  :cljsbuild {:builds {:dev
                       {:source-paths ["resources/src/cljs"]
                        :figwheel true
                        :compiler {:output-to "resources/public/cljs/main.js"
                                   :output-dir "resources/public/cljs/dev"
                                   :source-map true
                                   :main "slacky.app"
                                   :asset-path "/cljs/dev"
                                   :optimizations :none
                                   :pretty-print true}}
                       :prod
                       {:source-paths ["resources/src/cljs"]
                        :jar true
                        :compiler {:parallel-build true
                                   :output-to "resources/public/cljs/main.js"
                                   :output-dir "resources/public/cljs/prod"
                                   :main "slacky.app"
                                   :asset-path "/cljs/prod"
                                   :optimizations :advanced}}}}
  :profiles {:uberjar {:aot :all
                       :prep-tasks ["javac" "compile"
                                    ["shell" "./compile-memecaptain.sh"]
                                    ["with-profile" "dev" "cljsbuild" "once" "prod"]]}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [clj-http-fake "1.0.3"]
                                  [org.clojars.runa/conjure "2.2.0"]

                                  ;; cljs
                                  [org.clojure/clojurescript "1.9.908"]
                                  [figwheel-sidecar "0.5.13"]
                                  [com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [org.clojure/tools.reader "1.1.0"]
                                  [org.clojure/tools.trace "0.7.9"]

                                  [secretary "1.2.3"]
                                  [reagent "0.7.0"]
                                  [cljs-ajax "0.7.2"]]
                   :repl-options {:init-ns user
                                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :plugins [[lein-cljsbuild "1.1.2"]
                             [lein-figwheel "0.5.0-2"]
                             [lein-shell "0.5.0"]]}}
  :uberjar-name "slacky-standalone.jar")
