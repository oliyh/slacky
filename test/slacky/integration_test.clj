(ns slacky.integration-test
  (:require [clj-http.client :as http]
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

(deftest can-register-slack-account
  (let [token (clojure.string/replace (str (UUID/randomUUID)) "-" "")
        webhook-url "https://hooks.slack.com/services/foobarbaz"
        channel-name "my-channel"
        user-name "the-user"
        slack-promise (promise)]

    (cj/stubbing [memecaptain/create-template template-id
                  memecaptain/create-instance meme-url
                  google/image-search image-url
                  slack/send-message (fn [webhook-url channel message]
                                       (deliver slack-promise true))]

                 (is (= 200 (:status (http/post "http://localhost:8080/api/account"
                                                {:throw-exceptions? false
                                                 :form-params {:token token
                                                               :key webhook-url}}))))

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

                 (is (deref slack-promise 500 false))

                 (cj/verify-called-once-with-args slack/send-message webhook-url (str "#" channel-name) (slack/meme-message user-name "cats | cute cats | FTW" meme-url))
                 (cj/verify-called-once-with-args memecaptain/create-template "http://images.com/cat.jpg")
                 (cj/verify-called-once-with-args memecaptain/create-instance template-id "cute cats" "FTW"))))
