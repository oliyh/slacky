(ns slacky.accounts-test
  (:require [slacky
             [accounts :refer :all]
             [fixture :refer [with-database *db*]]]
            [clojure.test :refer :all]))

(use-fixtures :once with-database)

(deftest can-create-and-lookup-accounts
  (let [account-id (:id (add-account! *db* "foo"))]
    (is (integer? account-id))
    (is (= {:id account-id} (lookup-account *db* "foo")))

    (testing "can increment and read api stats"
      (is (= {:hits 0} (api-stats *db* account-id)))
      (api-hit! *db* account-id)
      (is (= {:hits 1} (api-stats *db* account-id))))))
