(ns jdbc.0005
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     (sql/create-table-ddl :browser_plugin_authentication
                           [:id "bigserial primary key"]
                           [:account_id "integer not null references accounts(id)"]
                           [:token "varchar(128)"])
     "CREATE UNIQUE INDEX browser_plugin_authentication_token_idx ON browser_plugin_authentication(token);")))

(defn down [db]
  (sql/with-db-transaction [db db]
    (sql/db-do-commands
     db
     "DROP TABLE IF EXISTS browser_plugin_authentication")))
