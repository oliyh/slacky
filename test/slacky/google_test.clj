(ns slacky.google-test
  (:require [clj-http.fake :refer :all]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [slacky
             [google :refer :all]]))

(deftest search-images-test
  (with-global-fake-routes
    {{:address image-search-url
      :query-params {:q "cats" :v 1.0 :rsz 8 :safe "active"}}
     {:get (fn [req] {:status 200 :headers {} :body (json/encode {:responseData {:results [{:unescapedUrl "cat-image.jpg"}]}})})}}

    (is (= "cat-image.jpg"
           (image-search "cats")))))
