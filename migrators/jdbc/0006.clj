(ns jdbc.0006
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     "ALTER TABLE accounts ADD COLUMN created_on TIMESTAMP NOT NULL DEFAULT now()")))

(defn down [db]
  (sql/with-db-transaction [db db]
    (sql/db-do-commands
     db
     "ALTER TABLE accounts DROP COLUMN created_on")))
