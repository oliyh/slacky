(ns jdbc.0007
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/with-db-transaction [db db]
      (sql/db-do-commands
       db
       [(sql/create-table-ddl :account_authentication
                              [[:id "bigserial primary key"]
                               [:account_id "integer not null references accounts(id)"]
                               [:token "varchar(128)"]])
        "CREATE UNIQUE INDEX account_authentication_token_idx ON account_authentication(token);"
        "INSERT INTO account_authentication (account_id, token) SELECT account_id, token FROM slack_authentication;"
        "INSERT INTO account_authentication (account_id, token) SELECT account_id, token FROM browser_plugin_authentication;"
        "DROP TABLE IF EXISTS slack_authentication"
        "DROP TABLE IF EXISTS browser_plugin_authentication"]))))

(defn down [db]
  (sql/with-db-transaction [db db]
    (sql/db-do-commands
     db
     ["DROP TABLE IF EXISTS account_authentication"])))
