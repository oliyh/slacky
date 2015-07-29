(ns slacky.fixture
  (:require [io.pedestal.http :as bootstrap]
            [slacky
             [server :as server]
             [db :refer [create-fresh-db-connection]]]))

(def test-database-url "jdbc:sqlite:memory:test")

(def ^:dynamic *db* nil)
(defn with-database [f]
  (binding [*db* (create-fresh-db-connection test-database-url)]
    (f)))

(defn with-web-api [f]
  (when server/service-instance
    (bootstrap/stop server/service-instance))

  (server/create-server {:db-url test-database-url
                         :pedestal-opts {:env :dev
                                         ::bootstrap/join? false}})
  (bootstrap/start server/service-instance)

  (try (f)
       (finally (bootstrap/stop server/service-instance))))
