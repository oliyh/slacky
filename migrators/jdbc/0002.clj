(ns jdbc.0002
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     [(sql/create-table-ddl :api_stats
                            [[:account_id "integer not null primary key references accounts(id)"]
                             [:hits "integer not null"]])
      "INSERT INTO api_stats (account_id, hits) SELECT id, 0 FROM accounts"])))

(defn down [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     [(sql/drop-table-ddl :api_stats)])))
