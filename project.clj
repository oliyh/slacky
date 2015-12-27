(defproject slacky "0.1.0-SNAPSHOT"
  :description "Memes-as-a-Service"
  :url "https://github.com/oliyh/slacky"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [frankiesardo/pedestal-swagger "0.4.3"]
                 [io.pedestal/pedestal.service "0.4.0"]
                 [io.pedestal/pedestal.jetty "0.4.0"]
                 [angel-interceptor "0.2.0"]

                 [ch.qos.logback/logback-classic "1.1.2" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.7"]
                 [org.slf4j/jcl-over-slf4j "1.7.7"]
                 [org.slf4j/log4j-over-slf4j "1.7.7"]

                 [org.clojure/tools.logging "0.3.1"]
                 [clj-http "1.1.2"]
                 [cheshire "5.4.0"]
                 [org.clojure/core.memoize "0.5.8"]


                 ;; web
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]

                 ;; persistence
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [com.h2database/h2 "1.4.189"]
                 [com.mchange/c3p0 "0.9.5.1"]
                 [joplin.core "0.2.12"]
                 [joplin.jdbc "0.2.12"]
                 [honeysql "0.6.1"]]
  :main ^:skip-aot slacky.server
  :min-lein-version "2.0.0"
  :target-path "target/%s"
  :resource-paths ["config", "resources", "migrators"]
  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [clj-http-fake "1.0.1"]
                                  [org.clojars.runa/conjure "2.1.3"]

                                  ;; cljs
                                  [org.clojure/clojurescript "1.7.107"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [org.clojure/tools.reader "0.10.0-alpha3"]
                                  [org.clojure/tools.trace "0.7.6"]

                                  [secretary "1.2.3"]
                                  [reagent "0.6.0-alpha"]]
                   :repl-options {:init-ns user
                                  ;; :timeout 200000
                                  ;; :host "0.0.0.0"
                                  ;; :port 44814
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :plugins [[lein-cljsbuild "1.0.6"]
                             [lein-figwheel "0.4.0"]]
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
                                         :compiler {:output-to "resources/public/cljs/main.js"
                                                    :output-dir "resources/public/cljs/prod"
                                                    :main "slacky.app"
                                                    :asset-path "/cljs/prod"
                                                    :optimizations :advanced}}}}}}
  :figwheel {:server-port 8083
             :nrepl-port 7888
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "cemerick.piggieback/wrap-cljs-repl"]}
  :uberjar-name "slacky-standalone.jar"
)
