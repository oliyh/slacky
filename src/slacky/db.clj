(ns slacky.db
  (:require [joplin.core :as joplin]
            [joplin.jdbc.database])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]
           org.sqlite.JDBC))

(defn db-provider
  "Utility fn used from migrators"
  [db]
  (let [url (get-in db [:db :url])]
    (condp re-find url
      #":sqlite:" :sqlite
      #":postgresql:" :postgres
      :unknown)))

(defn- migrate-db [url]
  (joplin/migrate-db
   {:db {:type :jdbc
         :url url}
    :migrator "migrators/jdbc"}))

(defn- wipe-db [url]
  (joplin/rollback-db
   {:db {:type :jdbc
         :url url}
    :migrator "migrators/jdbc"}
   Integer/MAX_VALUE))

(defn- pool [driver url]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass driver)
               (.setJdbcUrl url)

               (.setMinPoolSize 1)
               (.setMaxPoolSize 8)
               (.setAcquireIncrement 1)
               (.setPreferredTestQuery "select 1")

               (.setTestConnectionOnCheckout true)
               (.setCheckoutTimeout 7000)
               (.setAcquireRetryAttempts 3)
               (.setAcquireRetryDelay 1000)

               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))

(defn create-db-connection [url]
  (migrate-db url)

  (condp re-find url
    #":sqlite:" (pool "org.sqlite.JDBC" url)
    #":postgresql:" (pool "org.postgresql.Driver" url)

    (throw (RuntimeException. "Don't know what driver to use with" url))))

(defn create-fresh-db-connection [url]
  (wipe-db url)
  (create-db-connection url))
