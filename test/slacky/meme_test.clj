(ns slacky.meme-test
  (:require [slacky
             [accounts :refer [add-account!]]
             [fixture :refer [with-database *db*]]
             [meme :refer :all]
             [memecaptain :as memecaptain]]
            [clojure.test :refer :all]
            [conjure.core :as cj]))

(def resolve-meme #'slacky.meme/resolve-meme-pattern)
(use-fixtures :once with-database)

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

(def add-template #'slacky.meme/add-template)
(def resolve-meme-pattern #'slacky.meme/resolve-meme-pattern)

(deftest register-template-test
  (let [account-id (:id (add-account! *db*))
        result (promise)]

    (cj/stubbing [memecaptain/create-template "some-template-id"]

                 (add-template *db*
                               account-id
                               ":template angry martin http://foo.bar/baz.jpg"
                               (fn [& args] (deliver result (vec args))))

                 (is (= [:add-template "angry martin" "http://foo.bar/baz.jpg"]
                        (deref result 500 false)))
                 (cj/verify-called-once-with-args memecaptain/create-template "http://foo.bar/baz.jpg")

                 (is (= [:some-template-id "" "bidi!!!111one"]
                        (resolve-meme-pattern *db* account-id "angry martin | | bidi!!!111one"))))))
