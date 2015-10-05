(ns jdbc.0004
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     (sql/create-table-ddl :slack_authentication
                           [:id "bigserial primary key"]
                           [:account_id "integer not null references accounts(id)"]
                           [:token "varchar(128)"]
                           [:key "varchar(256)"])
     "CREATE UNIQUE INDEX slack_authentication_token_idx ON slack_authentication(token);"
     "INSERT INTO slack_authentication (account_id, token, key) SELECT id, token, key FROM accounts;"
     "ALTER TABLE accounts DROP COLUMN token"
     "ALTER TABLE accounts DROP COLUMN key")))

(defn down [db]
  (sql/with-db-transaction [db db]
    (sql/db-do-commands
     db
     "ALTER TABLE accounts ADD COLUMN token varchar(128);"
     "ALTER TABLE accounts ADD COLUMN key varchar(256);"
     "CREATE UNIQUE INDEX accounts_token_idx ON accounts(token);"
     "UPDATE accounts a SET token = (SELECT token FROM slack_authentication sa WHERE sa.account_id = a.id), key = (SELECT key FROM slack_authentication sa WHERE sa.account_id = a.id);"
     (sql/drop-table-ddl :slack_authentication))))
