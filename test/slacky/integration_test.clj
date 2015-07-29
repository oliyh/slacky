(ns slacky.integration-test
  (:require [clj-http.client :as http]
            [clj-http.fake :refer :all]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [slacky
             [fixture :refer [with-web-api]]
             [service :as service]
             [server :as server]
             [meme :refer [memecaptain-url image-search-url] :as meme]]))

(def memecaptain-template-polling-url (str memecaptain-url "/template-polling"))
(def memecaptain-meme-polling-url (str memecaptain-url "/meme-polling"))
(def meme-url (str memecaptain-url "/gend_images/a1jB3q.jpg"))
(def template-id "b7k3me")

(use-fixtures :once with-web-api)

(deftest can-generate-memes-with-memecaptain
  (with-global-fake-routes
    {memecaptain-template-polling-url
     {:get (fn [req] {:status 303 :headers {"location" "operation-complete"} :body ""})}

     memecaptain-meme-polling-url
     {:get (fn [req] {:status 303 :headers {"location" meme-url} :body ""})}

     (str memecaptain-url "/src_images")
     {:post (fn [req] {:status 202 :headers {"location" memecaptain-template-polling-url} :body (json/encode {:id template-id})})}

     (str memecaptain-url "/gend_images")
     {:post (fn [req]
              (if (= template-id (-> req :body slurp (json/decode keyword) :src_image_id))
                {:status 202 :headers {"location" memecaptain-meme-polling-url} :body "Follow the header"}
                {:status 500 :body "Template id is wrong!"}))}

     {:address image-search-url
      :query-params {:q "cats" :v 1.0 :rsz 8 :safe "active"}}
     {:get (fn [req] {:status 200 :headers {} :body (json/encode {:responseData {:results [{:unescapedUrl "cat-image.jpg"}]}})})}}

    (is (= meme-url
           (:body (http/post "http://localhost:8080/api/meme"
                             {:throw-exceptions? false
                              :form-params {:text "cats | cute cats | FTW"}}))))))
