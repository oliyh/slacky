(ns jdbc.0001
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     [(sql/create-table-ddl :accounts
                             [[:id "bigserial primary key"]
                              [:token "varchar(128)"]
                              [:key "varchar(256)"]])
      "CREATE UNIQUE INDEX accounts_token_idx ON accounts(token);"])))

(defn down [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     [(sql/drop-table-ddl :accounts)])))
