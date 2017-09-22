(ns jdbc.0003
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     [(sql/create-table-ddl :meme_templates
                            [[:id "bigserial primary key"]
                             [:account_id "integer not null references accounts(id)"]
                             [:name "varchar(128) not null"]
                             [:source_url "varchar(256) not null"]
                             [:template_id "varchar(128) not null"]])
      "CREATE INDEX meme_templates_name_idx ON meme_templates(name);"
      "CREATE INDEX meme_templates_account_idx ON meme_templates(account_id);"])))

(defn down [db]
  (sql/with-db-connection [db db]
    (sql/db-do-commands
     db
     [(sql/drop-table-ddl :meme_templates)])))
