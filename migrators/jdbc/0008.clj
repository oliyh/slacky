(ns jdbc.0008
  (:require [clojure.java.jdbc :as sql]
            [joplin.jdbc.database]))

(defn up [db]
  (sql/with-db-connection [db db]
    (sql/with-db-transaction [db db]
      (sql/db-do-commands
       db
       (sql/create-table-ddl :slack_app_authentication
                             [:id "bigserial primary key"]
                             [:account_id "integer not null references accounts(id)"]
                             [:team_name "varchar(128)"]
                             [:team_id "varchar(128)"]
                             [:access_token "varchar(128)"]
                             [:webhook_url "varchar(128)"]
                             [:webhook_channel "varchar(128)"]
                             [:webhook_config_url "varchar(128)"])
       "CREATE UNIQUE INDEX slack_app_authentication_team_name_idx ON slack_app_authentication(team_name);"))))

(defn down [db]
  (sql/with-db-transaction [db db]
    (sql/db-do-commands
     db
     "DROP TABLE IF EXISTS slack_app_authentication")))
