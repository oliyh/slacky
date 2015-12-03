(ns slacky.bing-test
  (:require [clj-http.fake :refer :all]
            [clojure.test :refer :all]
            [cheshire.core :as json]
            [slacky
             [bing :refer :all]]))

(deftest search-images-test
  (with-global-fake-routes
    {{:address image-search-url
      :query-params {:q "cats"}}
     {:get (fn [req] {:status 200
                      :headers {}
                      :body (slurp "test/slacky/bing-image-response.html")})}}

    (is (= "http://cats.com/cat-image.jpg"
           (image-search "cats")))))
