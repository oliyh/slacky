(ns slacky.memecaptain-test
  (:require [slacky.memecaptain :refer [create-direct]]
            [clojure.java.io :as io]
            [clj-http.fake :as http-fake]
            [clojure.java.shell :as sh]
            [clojure.test :refer [deftest is]])
  (:import (java.nio.file Files)))

(deftest can-generate-meme
  (let [image-url "http://foo.com/meme-image.jpg"
        image-file (io/file (io/resource "public/images/businesscat.png"))
        image-contents (Files/readAllBytes (.toPath image-file))
        meme-file (atom nil)]
    (http-fake/with-global-fake-routes
      {image-url
       {:get (constantly {:status 200 :body image-contents})}}
      (with-redefs [sh/sh (fn [exe image-file _o output-file _ _font-file _t text-upper _b text-lower]
                            (is (= "./bin/memecaptain" exe))
                            (is image-file)
                            (is (= "slacky" text-upper))
                            (is (= "test" text-lower))
                            (reset! meme-file output-file)
                            (io/copy (io/file image-file) (io/file output-file))
                            {:exit 0})]

        (let [meme-path (create-direct image-url "slacky" "test")]
          (is meme-path)
          (is (= (slurp image-file)
                 (slurp @meme-file))))))))
