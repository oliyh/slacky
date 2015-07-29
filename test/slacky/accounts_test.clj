(ns slacky.accounts-test
  (:require [slacky
             [accounts :refer :all]
             [fixture :refer [with-database *db*]]]
            [clojure.test :refer :all]))

(use-fixtures :once with-database)

(deftest can-create-and-lookup-accounts
  (add-account! *db* "foo" "bar")

  (is (= {:id 1
          :token "foo"
          :key "bar"}
         (lookup-account *db* "foo"))))
