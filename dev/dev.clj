(ns dev
  (:require [angel.interceptor :as angel]
            [cemerick.piggieback :as piggieback]
            [clojure.test]
            [clojure.tools.namespace.repl :as repl]
            [io.pedestal.http :as bootstrap]
            [slacky.db :as db]
            [slacky
             [service :as service]
             [server :as server]
             [settings :as settings]]))

(def clear repl/clear)
(def refresh repl/refresh)

(defn cljs-repl []
  (piggieback/cljs-repl (cljs.repl.rhino/repl-env)))

(def service (-> service/service ;; start with production configuration
                 (merge  {:env :dev
                          ;; do not block thread that starts web server
                          ::bootstrap/join? false
                          ;; reload routes on every request
                          ::bootstrap/routes #(deref #'service/routes)
                          ;; all origins are allowed in dev mode
                          ::bootstrap/allowed-origins (constantly true)})
                 angel/satisfy
                 (bootstrap/default-interceptors)
                 (bootstrap/dev-interceptors)))

(defn start [& [opts]]
  (server/create-server {:pedestal-opts (merge service opts)})
  (bootstrap/start server/service-instance))

(defn stop []
  (when server/service-instance
    (bootstrap/stop server/service-instance)))

(defn run-all-tests []
  (stop)
  (refresh)
  (clojure.test/run-all-tests #"slacky.*test$"))
