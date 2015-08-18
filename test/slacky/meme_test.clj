(ns slacky.meme-test
  (:require [slacky.meme :refer :all]
            [clojure.test :refer :all]))

(def resolve-meme #'slacky.meme/resolve-meme-pattern)

(deftest resolve-meme-pattern-test
  (are [text meme-pattern] (= meme-pattern (resolve-meme text))

    "y u no foos?" [:NryNmg "y u no" "foos?"]
    "one does not simply photograph the photographer" [:da2i4A "one does not simply" "photograph the photographer"]

    "ceiling cat | ceiling cat | watching you" ["ceiling cat" "ceiling cat" "watching you"]

    "angry arnold | | thiiiings!!11one" ["angry arnold" "" "thiiiings!!11one"]
    "angry arnold | thiiiings!!11one |" ["angry arnold" "thiiiings!!11one" ""]
    "angry arnold | |" ["angry arnold" "" ""]

    "angry arnold | thiiiings!!11one" nil ;; might be nice to implement this
    "angry arnold |" nil
    "angry arnold" nil
    ))

(deftest describe-meme-patterns-test
  (let [descriptions (describe-meme-patterns)]
    (is (= {:pattern "y u no [lower]"
            :template "http://i.memecaptain.com/src_images/NryNmg.jpg"}
           (first descriptions)))))
