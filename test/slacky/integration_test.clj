(ns slacky.integration-test
  (:require [clj-http.client :as http]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [conjure.core :as cj]
            [slacky
             [memecaptain :as memecaptain]
             [fixture :refer [with-web-api with-database with-fake-internet]]
             [service :as service]
             [settings :refer [web-port]]
             [google :as google]
             [slack :as slack]])
  (:import [java.util UUID]))

(use-fixtures :once with-web-api with-database)

(defn- slacky-url [path]
  (format "http://localhost:%s%s" (web-port) path))

(deftest can-generate-memes
  (with-fake-internet {}
    (is (= meme-url
           (:body (http/post (slacky-url "/api/meme")
                             {:throw-exceptions? false
                              :form-params {:text "cats | cute cats | FTW"}}))))

    (cj/verify-called-once-with-args memecaptain/create-template "http://images.com/cat.jpg")
    (cj/verify-called-once-with-args memecaptain/create-instance template-id "cute cats" "FTW"))

  (testing "400 when bad meme syntax"
    (with-fake-internet {}
      (let [response (http/post (slacky-url "/api/meme")
                                {:throw-exceptions? false
                                 :form-params {:text "nil"}})]
        (is (= 400 (:status response)))
        (is (= "Sorry, the command was not recognised"
               (:body response))))

      (cj/verify-call-times-for google/image-search 0)
      (cj/verify-call-times-for memecaptain/create-template 0)
      (cj/verify-call-times-for memecaptain/create-instance 0))))


(def basic-help-message
  (str "Create a meme using one of the following patterns:\n"
       "[search terms or image url] | [upper] | [lower] \n"
       "y u no [lower]\n"
       "one does not simply [lower]\n"
       "not sure if [upper] or [lower]\n"
       "brace yoursel(f|ves) [lower]\n"
       "success when [upper] then [lower]\n"
       "cry when [upper] then [lower]\n"
       "what if i told you [lower]\n"
       "[upper] how do they work?\n"
       "[upper] all the [lower]\n"
       "[upper] [upper] everywhere\n\n"

       "Create a template to use in memes:\n"
       "/meme :template [name of template] https://cats.com/cat.jpg"))


(deftest can-integrate-with-slack
  (let [token (clojure.string/replace (str (UUID/randomUUID)) "-" "")
        response-url "https://hooks.slack.com/services/foobarbaz"
        channel-name "my-channel"
        user-name "the-user"
        slack-post! (fn [text]
                      (:body (http/post (slacky-url "/api/slack/meme")
                                        {:throw-exceptions? false
                                         :form-params {:token token
                                                       :team_id "a"
                                                       :team_domain "b"
                                                       :channel_id "c"
                                                       :channel_name channel-name
                                                       :user_id "d"
                                                       :user_name user-name
                                                       :command "/meme"
                                                       :text text
                                                       :response_url response-url}})))]

    (testing "alerts on bad syntax"
      (with-fake-internet {}
        (is (= "Sorry, the command was not recognised, try '/meme :help' for help"
               (slack-post! "how does this work?")))))

    (testing "can ask for help"
      (with-fake-internet {}
        (is (= basic-help-message
               (slack-post! ":help")))))

    (testing "can meme from a channel"
      (with-fake-internet {}
        (is (= "Your meme is on its way"
               (slack-post! "cats | cute cats | FTW")))

        (is (= [response-url "in_channel"
                (slack/->message :meme user-name "cats | cute cats | FTW" meme-url)]
               (first (a/alts!! [slack-channel (a/timeout 500)]))))))

    (testing "can register a template"
      (with-fake-internet {:template-id "cute-cat-template-id"}
        (is (= "Your template is being registered"
               (slack-post! ":template cute cats http://cats.com/cute.jpg")))

        (is (= [response-url "in_channel"
                (slack/->message :add-template user-name nil
                                 "cute cats" "http://cats.com/cute.jpg")]
               (first (a/alts!! [slack-channel (a/timeout 500)]))))

        (cj/verify-first-call-args-for memecaptain/create-template "http://cats.com/cute.jpg")

        (testing "and can use it in a meme"
          (is (= "Your meme is on its way"
                 (slack-post! "cute cats | omg | so cute")))

          (is (= [response-url "in_channel"
                  (slack/->message :meme user-name "cute cats | omg | so cute" meme-url)]
                 (first (a/alts!! [slack-channel (a/timeout 500)]))))

          (cj/verify-call-times-for memecaptain/create-template 1)
          (cj/verify-first-call-args-for memecaptain/create-instance template-id "omg" "so cute")))

      (testing "templates show up in help message"
        (with-fake-internet {}
          (is (= (str basic-help-message
                      "\n\n"
                      (string/join "\n"
                                   ["Custom templates:"
                                    "cute cats"]))
                 (slack-post! ":help"))))))))


(deftest can-integrate-with-browser-plugins
  (with-fake-internet {}
    (is (= meme-url
           (:body (http/post (slacky-url "/api/browser-plugin/meme")
                             {:throw-exceptions? false
                              :form-params {:token (str (UUID/randomUUID))
                                            :text "cats | cute cats | FTW"}}))))

    (cj/verify-called-once-with-args memecaptain/create-template "http://images.com/cat.jpg")
    (cj/verify-called-once-with-args memecaptain/create-instance template-id "cute cats" "FTW"))

  (testing "400 when bad meme syntax"
    (with-fake-internet {}
      (let [response (http/post (slacky-url "/api/browser-plugin/meme")
                                {:throw-exceptions? false
                                 :form-params {:token (str (UUID/randomUUID))
                                               :text "nil"}})]
        (is (= 400 (:status response)))
        (is (= "Sorry, the command was not recognised"
               (:body response))))

      (cj/verify-call-times-for google/image-search 0)
      (cj/verify-call-times-for memecaptain/create-template 0)
      (cj/verify-call-times-for memecaptain/create-instance 0))))
