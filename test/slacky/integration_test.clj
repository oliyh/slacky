(ns slacky.integration-test
  (:require [clj-http.client :as http]
            [clojure.core.async :as a]
            [clojure.test :refer :all]
            [conjure.core :as cj]
            [slacky
             [memecaptain :as memecaptain]
             [fixture :refer [with-web-api]]
             [service :as service]
             [google :as google]
             [slack :as slack]])
  (:import [java.util UUID]))

(def image-url "http://images.com/cat.jpg")
(def meme-url (str memecaptain/memecaptain-url "/gend_images/a1jB3q.jpg"))
(def template-id "b7k3me")

(use-fixtures :once with-web-api)

(deftest can-generate-memes
  (cj/stubbing [memecaptain/create-template template-id
                memecaptain/create-instance meme-url
                google/image-search image-url]

               (is (= meme-url
                      (:body (http/post "http://localhost:8080/api/meme"
                                        {:throw-exceptions? false
                                         :form-params {:text "cats | cute cats | FTW"}}))))

               (cj/verify-called-once-with-args memecaptain/create-template "http://images.com/cat.jpg")
               (cj/verify-called-once-with-args memecaptain/create-instance template-id "cute cats" "FTW")))

(deftest can-integrate-with-slack
  (let [token (clojure.string/replace (str (UUID/randomUUID)) "-" "")
        webhook-url "https://hooks.slack.com/services/foobarbaz"
        channel-name "my-channel"
        user-name "the-user"
        slack-messages (a/chan)]

    (cj/stubbing [memecaptain/create-template template-id
                  memecaptain/create-instance meme-url
                  google/image-search image-url
                  slack/send-message (fn [& args]
                                       (a/put! slack-messages args))]

                 (testing "can create an account"
                   (is (= 200 (:status (http/post "http://localhost:8080/api/account"
                                                  {:throw-exceptions? false
                                                   :form-params {:token token
                                                                 :key webhook-url}})))))

                 (testing "can meme from a channel"
                   (is (= "Your meme is on its way"
                          (:body (http/post "http://localhost:8080/api/slack/meme"
                                            {:throw-exceptions? false
                                             :form-params {:token token
                                                           :team_id "a"
                                                           :team_domain "b"
                                                           :channel_id "c"
                                                           :channel_name channel-name
                                                           :user_id "d"
                                                           :user_name user-name
                                                           :command "/meme"
                                                           :text "cats | cute cats | FTW"}}))))

                   (is (= [webhook-url (str "#" channel-name) (slack/meme-message user-name "cats | cute cats | FTW" meme-url)]
                          (first (a/alts!! [slack-messages (a/timeout 500)]))))))))
