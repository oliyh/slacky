(ns slacky.templates
  (:refer-clojure :exclude [list])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [honeysql.core :as sql]))

(defn lookup [db account-id name]
  (->
   (jdbc/query db (sql/format {:select [:source_url]
                               :from [:meme_templates]
                               :where [:and
                                       [:= :account_id account-id]
                                       [:= :name (-> name string/trim string/lower-case)]]
                               :limit 1}))
   first
   :source_url))

(defn persist! [db account-id template-name source-url]
  (jdbc/with-db-transaction [db db]
    (jdbc/insert! db :meme_templates {:account_id account-id
                                      :name (-> template-name string/trim string/lower-case)
                                      :source_url source-url})))

(defn delete! [db account-id template-name]
  (jdbc/with-db-transaction [db db]
    (jdbc/delete! db :meme_templates ["account_id = ? AND name = ?"
                                      account-id
                                      (-> template-name string/trim string/lower-case)])))

(defn list [db account-id]
  (->>
   (jdbc/query db (sql/format {:select [:name :source_url]
                               :from [:meme_templates]
                               :where [:= :account_id account-id]}))
   (map #(rename-keys % {:source_url :source-url}))))
