(ns slacky.accounts-test
  (:require [slacky.accounts :refer :all]
            [slacky.db :refer [create-fresh-db-connection]]
            [clojure.test :refer :all]))

(def ^:dynamic *db* nil)
(defn- with-database [f]
  (binding [*db* (create-fresh-db-connection "jdbc:sqlite:memory:test")]
    (f)))

(use-fixtures :once with-database)

(deftest can-create-and-lookup-accounts
  (add-account! *db* "foo" "bar")

  (is (= {:id 1
          :token "foo"
          :key "bar"}
         (lookup-account *db* "foo"))))
