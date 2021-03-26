(ns slacky.bing-test
  (:require [clj-http.fake :refer [with-global-fake-routes]]
            [clojure.test :refer [deftest testing is]]
            [slacky.bing :as bing]))

(deftest search-images-test
  (testing "normal image search"
    (with-global-fake-routes
      {{:address bing/image-search-url
        :query-params {:q "cats"}}
       {:get (fn [_req] {:status 200
                         :headers {}
                         :body (slurp "test/clj/slacky/bing-image-response.html")})}}

      (is (= "http://cats.com/cat-image.jpg"
             (bing/image-search "cats")))))

  (testing "animated image search"
    (with-global-fake-routes
      {{:address bing/image-search-url
        :query-params {:q "cats"
                       :qft "+filterui:photo-animatedgif"}}
       {:get (fn [_req] {:status 200
                         :headers {}
                         :body (slurp "test/clj/slacky/bing-image-response.html")})}}

      (is (= "http://cats.com/cat-image.jpg"
             (bing/image-search "cats :anim"))))))
