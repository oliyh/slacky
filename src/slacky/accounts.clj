(ns slacky.accounts
  (:require [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [clojure.tools.logging :as log]
            [slacky.db :refer [db-provider]])
  (:import [java.sql Statement Timestamp]))

(defn lookup-slack-account [db token]
  (first (jdbc/query db (sql/format {:select [[:account_id :id]
                                              :key]
                                     :from [:slack_authentication]
                                     :where [:= :token token]
                                     :limit 1}))))

(defn lookup-browser-plugin-account [db token]
  (first (jdbc/query db (sql/format {:select [[:account_id :id]]
                                     :from [:browser_plugin_authentication]
                                     :where [:= :token token]
                                     :limit 1}))))

(defn add-authentication! [db account-id authentications]
  (doseq [[type auth] authentications]
    (condp = type
      :slack (jdbc/insert! db :slack_authentication (merge {:account_id account-id} auth))
      :browser-plugin (jdbc/insert! db :browser_plugin_authentication (merge {:account_id account-id} auth)))))

(defn add-account! [db & [authentications]]
  (jdbc/with-db-transaction [db db]
    (let [result (jdbc/insert! db :accounts {:created_on (java.sql.Timestamp. (System/currentTimeMillis))})
          id (-> result first vals first)]
      (log/warn "account insert result")
      (log/warn "id is" id)
      (add-authentication! db id authentications)
      (jdbc/insert! db :api_stats {:account_id id
                                   :hits 0})
      {:id id})))

(defn api-hit! [db ^Number account-id]
  (let [command "UPDATE api_stats SET hits = hits + 1 WHERE account_id = ?"]
    (jdbc/execute! db [command account-id])))

(defn api-stats [db account-id]
  (first (jdbc/query db (sql/format {:select [:hits]
                                     :from [:api_stats]
                                     :where [:= :account_id account-id]
                                     :limit 1}))))
