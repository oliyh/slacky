(ns slacky.accounts
  (:require [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql])
  (:import [java.sql Statement]))

(defn lookup-account [db token]
  (first (jdbc/query db (sql/format {:select [[:account_id :id]
                                              :key]
                                     :from [:slack_authentication]
                                     :where [:= :token token]
                                     :limit 1}))))

(defmulti add-authentication! (fn [db account-id type & args] type))

(defmethod add-authentication! :slack [db account-id _ token key]
  (jdbc/insert! db :slack_authentication {:account_id account-id :token token :key key}))

(defn add-account! [db token key]
  (jdbc/with-db-transaction [db db]
    (let [id (-> (jdbc/insert! db :accounts {}) first vals first)]
      (add-authentication! db id :slack token key)
      (jdbc/insert! db :api_stats {:account_id id
                                   :hits 0}))))

(defn api-hit! [db ^Number account-id]
  (let [command "UPDATE api_stats SET hits = hits + 1 WHERE account_id = ?"]
    (jdbc/execute! db [command account-id])))

(defn api-stats [db account-id]
  (first (jdbc/query db (sql/format {:select [:hits]
                                     :from [:api_stats]
                                     :where [:= :account_id account-id]
                                     :limit 1}))))
