(ns slacky.memecaptain-test
  (:require [slacky.memecaptain :as memecaptain]
            [clojure.java.io :as io]
            [clj-http.fake :as http-fake]
            [clojure.test :refer [deftest is use-fixtures testing]])
  (:import (java.nio.file Files)
           (javax.imageio ImageIO)))

(use-fixtures :once
  (fn [tests]
    (memecaptain/init)
    (tests)))

(deftest can-generate-meme
  (let [image-file (io/file (io/resource "public/images/businesscat.png"))
        image-contents (Files/readAllBytes (.toPath image-file))
        fake-url "http://test.local/template.png"]
    (http-fake/with-global-fake-routes
      {fake-url {:get (constantly {:status 200 :body image-contents})}}

      (testing "generates a meme with top and bottom text"
        (let [meme-path (memecaptain/create-direct fake-url "hello" "world")
              meme-file (io/file meme-path)]
          (try
            (is (.exists meme-file) "meme file should exist")
            (is (> (.length meme-file) 0) "meme file should not be empty")

            (let [meme-image (ImageIO/read meme-file)]
              (is (some? meme-image) "meme should be a valid image")
              (is (= (.getWidth (ImageIO/read image-file))
                     (.getWidth meme-image))
                  "meme should have same width as template")
              (is (= (.getHeight (ImageIO/read image-file))
                     (.getHeight meme-image))
                  "meme should have same height as template"))
            (finally
              (.delete meme-file)))))

      (testing "handles empty text"
        (let [meme-path (memecaptain/create-direct fake-url "" "bottom only")
              meme-file (io/file meme-path)]
          (try
            (is (.exists meme-file))
            (is (some? (ImageIO/read meme-file)))
            (finally
              (.delete meme-file)))))

      (testing "handles long text by scaling font"
        (let [long-text "this is a very long piece of text that should be scaled down"
              meme-path (memecaptain/create-direct fake-url long-text "short")
              meme-file (io/file meme-path)]
          (try
            (is (.exists meme-file))
            (is (some? (ImageIO/read meme-file)))
            (finally
              (.delete meme-file))))))))
