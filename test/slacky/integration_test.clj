(ns slacky.integration-test
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [conjure.core :as cj]
            [slacky
             [memecaptain :as memecaptain]
             [fixture :refer [with-web-api]]
             [service :as service]
             [google :as google]]))

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
