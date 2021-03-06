(ns dev
  (:require [angel.interceptor :as angel]
            [clojure.test]
            [clojure.tools.namespace.repl :as repl]
            [io.pedestal.http :as bootstrap]
            [slacky
             [memecaptain :as memecaptain]
             [service :as service]
             [server :as server]
             [settings :as settings]]))

(def clear repl/clear)
(def refresh repl/refresh)

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
  (memecaptain/init)
  (server/create-server {:pedestal-opts (merge service opts)})
  (bootstrap/start server/service-instance))

(defn stop []
  (when server/service-instance
    (bootstrap/stop server/service-instance)))

(defn reset []
  (stop)
  (refresh)
  (start))

(defn run-all-tests []
  (stop)
  (refresh)
  (clojure.test/run-all-tests #"slacky.*test$"))
