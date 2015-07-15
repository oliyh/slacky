(ns slacky.accounts
  (:require [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]))

(defn add-account! [db token key]
  (jdbc/insert! db "accounts" {:token token
                               :key key}))

(defn lookup-account [db token]
  (first (jdbc/query db
                     (sql/format {:select [:*]
                                  :from [:accounts]
                                  :where [:= :token token]
                                  :limit 1}))))
