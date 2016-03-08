(defproject slacky "0.1.0-SNAPSHOT"
  :description "Memes-as-a-Service"
  :url "https://github.com/oliyh/slacky"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [pedestal-api "0.1.0"]
                 [io.pedestal/pedestal.service "0.4.1"]
                 [io.pedestal/pedestal.jetty "0.4.1"]
                 [angel-interceptor "0.2.0"]

                 [ch.qos.logback/logback-classic "1.1.6" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.18"]
                 [org.slf4j/jcl-over-slf4j "1.7.18"]
                 [org.slf4j/log4j-over-slf4j "1.7.18"]

                 [org.clojure/tools.logging "0.3.1"]
                 [clj-http "2.1.0"]
                 [cheshire "5.5.0"]
                 [org.clojure/core.memoize "0.5.8"]

                 ;; web
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]

                 ;; persistence
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.postgresql/postgresql "9.4.1208"]
                 [com.h2database/h2 "1.4.191"]
                 [com.mchange/c3p0 "0.9.5.2"]
                 [joplin.core "0.3.6"]
                 [joplin.jdbc "0.3.6"]
                 [honeysql "0.6.3"]]
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
                        :parallel-build true
                        :jar true
                        :compiler {:output-to "resources/public/cljs/main.js"
                                   :output-dir "resources/public/cljs/prod"
                                   :main "slacky.app"
                                   :asset-path "/cljs/prod"
                                   :optimizations :advanced}}}}
  :profiles {:uberjar {:aot :all
                       :prep-tasks ["javac" "compile" ["with-profile" "dev" "cljsbuild" "once" "prod"]]}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [clj-http-fake "1.0.2"]
                                  [org.clojars.runa/conjure "2.2.0"]

                                  ;; cljs
                                  [org.clojure/clojurescript "1.7.228"]
                                  [figwheel-sidecar "0.5.0-6"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.12"]
                                  [org.clojure/tools.reader "0.10.0"]
                                  [org.clojure/tools.trace "0.7.9"]

                                  [secretary "1.2.3"]
                                  [reagent "0.6.0-alpha"]
                                  [cljs-ajax "0.5.3"]]
                   :repl-options {:init-ns user
                                   :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :plugins [[lein-cljsbuild "1.1.2"]
                             [lein-figwheel "0.5.0-2"]]}}
  :uberjar-name "slacky-standalone.jar")
