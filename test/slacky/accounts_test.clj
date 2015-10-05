(ns slacky.accounts-test
  (:require [slacky
             [accounts :refer :all]
             [fixture :refer [with-database *db*]]]
            [clojure.test :refer :all]))

(use-fixtures :once with-database)

(deftest can-create-and-lookup-accounts
  (add-account! *db* "foo" "bar")

  (is (= {:id 1
          :key "bar"}
         (lookup-account *db* "foo")))

  (testing "can increment and read api stats"
    (is (= {:hits 0} (api-stats *db* 1)))
    (api-hit! *db* 1)
    (is (= {:hits 1} (api-stats *db* 1)))))
