(ns jdbc.0004
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]
            [slacky.db :refer [db-provider]]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     (sql/create-table-ddl :slack_auth
                           [:id (condp = (db-provider db)
                                  :sqlite "integer primary key"
                                  :postgres "bigserial primary key")]
                           [:account_id "integer not null references accounts(id)"]
                           [:token "varchar(128)"]
                           [:key "varchar(256)"])
     "CREATE INDEX slack_auth_token_idx ON account(token);"
     "CREATE INDEX slack_auth_account_idx ON slack_auth(account_id);"

     "INSERT INTO slack_auth (account_id, token, key)
         SELECT id, token, key FROM accounts;"

     "ALTER TABLE accounts DROP COLUMN token, DROP COLUMN key;")))

(defn down [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     "ALTER TABLE accounts
          ADD COLUMN token varchar(128),
          ADD COLUMN key varchar(256)"
     "UPDATE accounts a
          SET token = (SELECT token FROM slack_auth sa WHERE sa.account_id = a.id,
          SET key = (SELECT key FROM slack_auth sa WHERE sa.account_id = a.id;"
     (sql/drop-table-ddl :slack_auth))))
