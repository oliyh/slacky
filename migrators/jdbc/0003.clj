(ns jdbc.0003
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]
            [slacky.db :refer [db-provider]]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     (sql/create-table-ddl :meme_templates
                           [:id (condp = (db-provider db)
                                  :sqlite "integer primary key"
                                  :postgres "bigserial primary key")]
                           [:account_id "integer not null references accounts(id)"]
                           [:name "varchar(128) not null"]
                           [:source_url "varchar(256) not null"]
                           [:template_id "varchar(128) not null"])
     "CREATE UNIQUE INDEX meme_templates_name_idx ON meme_templates(name);"
     "CREATE UNIQUE INDEX meme_templates_account_idx ON meme_templates(account_id);")))

(defn down [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     (sql/drop-table-ddl :meme_templates))))
