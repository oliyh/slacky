(ns slacky.accounts
  (:require [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [clojure.tools.logging :as log]
            [slacky.db :refer [db-provider]]
            [clojure.string :as string])
  (:import [java.sql Statement Timestamp]))

(defn- ->snake [m]
  (reduce-kv
   (fn [acc k v]
     (assoc acc (-> k name (string/replace "-" "_") keyword) v))
   {}
   m))

(defn lookup-basic-account [db token]
  (first (jdbc/query db (sql/format {:select [[:account_id :id]]
                                     :from [:account_authentication]
                                     :where [:= :token token]
                                     :limit 1}))))

(defn lookup-slack-account [db team-id]
  (first (jdbc/query db (sql/format {:select [[:account_id :id]]
                                     :from [:slack_app_authentication]
                                     :where [:= :team_id team-id]
                                     :limit 1}))))

(defn- create-account! [db]
  (jdbc/with-db-transaction [db db]
    (let [result (jdbc/insert! db :accounts {:created_on (java.sql.Timestamp. (System/currentTimeMillis))})
          id (-> result first vals first)]
      (log/warn "New account id is" id)
      (jdbc/insert! db :api_stats {:account_id id
                                   :hits 0})
      {:id id})))

(defn register-basic-account! [db token]
  (jdbc/with-db-transaction [db db]
    (let [account (create-account! db)]
      (jdbc/insert! db :account_authentication {:account_id (:id account) :token token})
      account)))

(defn register-slack-account! [db api-access]
  (jdbc/with-db-transaction [db db]
    (let [account (create-account! db)]
      (jdbc/insert! db :slack_app_authentication
                    (merge
                     {:account_id (:id account)}
                     (->snake api-access)))
      account)))

(defn api-hit! [db ^Number account-id]
  (let [command "UPDATE api_stats SET hits = hits + 1 WHERE account_id = ?"]
    (jdbc/execute! db [command account-id])))

(defn api-stats [db account-id]
  (first (jdbc/query db (sql/format {:select [:hits]
                                     :from [:api_stats]
                                     :where [:= :account_id account-id]
                                     :limit 1}))))
