(ns slacky.accounts-test
  (:require [slacky
             [accounts :refer :all]
             [fixture :refer [with-database *db*]]]
            [clojure.test :refer :all]))

(use-fixtures :once with-database)

(deftest can-create-and-lookup-basic-accounts
  (let [account-id (:id (register-basic-account! *db* "foo"))]
    (is (integer? account-id))
    (is (= {:id account-id} (lookup-basic-account *db* "foo")))

    (testing "can increment and read api stats"
      (is (= {:hits 0} (api-stats *db* account-id)))
      (api-hit! *db* account-id)
      (is (= {:hits 1} (api-stats *db* account-id))))))

(deftest can-create-and-lookup-slack-accounts
  (let [account-id (:id (register-slack-account!
                         *db*
                         {:team-name "team-slacky"
                          :team-id "XXXXXX001"
                          :access-token "xoxp-XXXXXXXX-XXXXXXXX-XXXXX"
                          :webhook-url "https://hooks.slack.com/TXXXXX/BXXXXX/XXXXXXXXXX"
                          :webhook-channel "#channel-it-will-post-to"
                          :webhook-config-url "https://teamname.slack.com/services/BXXXXX"}))]
    (is (integer? account-id))
    (is (= {:id account-id} (lookup-slack-account *db* "XXXXXX001")))

    (testing "can increment and read api stats"
      (is (= {:hits 0} (api-stats *db* account-id)))
      (api-hit! *db* account-id)
      (is (= {:hits 1} (api-stats *db* account-id))))))

(deftest can-convert-basic-account-to-slack-account
  (let [account-id (:id (register-basic-account! *db* "foo2"))]
    (is (= {:id account-id} (lookup-basic-account *db* "foo2")))

    (convert-to-slack-account! *db* account-id {:team-id "XXXXXX002"})

    (is (= {:id account-id} (lookup-slack-account *db* "XXXXXX002")))
    (is (= {:id account-id} (lookup-basic-account *db* "foo2")))))
