(ns jdbc.0009
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     ["ALTER TABLE meme_templates DROP COLUMN template_id"])))

(defn down [db]
  (sql/with-db-transaction [db db]
    (sql/db-do-commands
     db
     ["ALTER TABLE meme_templates ADD COLUMN template_id varchar(128) not null;"])))
