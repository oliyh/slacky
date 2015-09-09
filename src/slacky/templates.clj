(ns slacky.templates
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [honeysql.core :as sql]))

(defn lookup [db account-id name]
  (->
   (jdbc/query db (sql/format {:select [:template_id]
                               :from [:meme_templates]
                               :where [:and
                                       [:= :account_id account-id]
                                       [:= :name (-> name string/trim string/lower-case)]]
                               :limit 1}))
   first
   :template_id
   keyword))

(defn persist! [db account-id name source-url template-id]
  (jdbc/with-db-transaction [db db]
    (jdbc/insert! db :meme_templates {:account_id account-id
                                      :name (-> name string/trim string/lower-case)
                                      :source_url source-url
                                      :template_id template-id})))
