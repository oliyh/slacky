(ns slacky.memecaptain-test
  (:require [clj-http.fake :refer :all]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [slacky
             [memecaptain :refer :all]]))

(def memecaptain-template-polling-url (str memecaptain-url "/template-polling"))
(def memecaptain-meme-polling-url (str memecaptain-url "/meme-polling"))
(def meme-url (str memecaptain-url "/gend_images/a1jB3q.jpg"))
(def template-id "b7k3me")

(deftest can-generate-memes
  (with-global-fake-routes
    {memecaptain-meme-polling-url
     {:get (fn [req] {:status 303 :headers {"location" meme-url} :body ""})}

     (str memecaptain-url "/gend_images")
     {:post (fn [req]
              (if (= template-id (-> req :body slurp (json/decode keyword) :src_image_id))
                {:status 202 :headers {"location" memecaptain-meme-polling-url} :body "Follow the header"}
                {:status 500 :body "Template id is wrong!"}))}}

    (is (= meme-url
           (create-instance template-id "cute cats" "FTW")))))

(deftest can-generate-templates
  (with-global-fake-routes
    {memecaptain-template-polling-url
     {:get (fn [req] {:status 303 :headers {"location" "operation-complete"} :body ""})}

     (str memecaptain-url "/src_images")
     {:post (fn [req] {:status 202 :headers {"location" memecaptain-template-polling-url} :body (json/encode {:id template-id})})}}

    (is (= template-id
           (create-template "cats")))))
