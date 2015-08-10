(ns slacky.meme-test
  (:require [slacky.meme :refer :all]
            [clojure.test :refer :all]))

(def resolve-meme #'slacky.meme/resolve-meme-pattern)

(deftest resolve-meme-pattern-test
  (are [text meme-pattern] (= meme-pattern (resolve-meme text))

    "y u no foos?" [:y-u-no "y u no" "foos?"]

    "ceiling cat | ceiling cat | watching you" ["ceiling cat" "ceiling cat" "watching you"]

    "angry arnold | | thiiiings!!11one" ["angry arnold" "" "thiiiings!!11one"]
    "angry arnold | thiiiings!!11one |" ["angry arnold" "thiiiings!!11one" ""]

    "angry arnold | thiiiings!!11one" nil ;; might be nice to implement this
    "angry arnold | |" nil
    "angry arnold |" nil
    "angry arnold" nil
    ))
