(ns slacky.meme-test
  (:require [clojure.core.async :as a]
            [clojure.test :refer :all]
            [conjure.core :as cj]
            [slacky
             [accounts :refer [register-basic-account!]]
             [fixture :refer [with-database *db*]]
             [meme :refer :all]
             [memecaptain :as memecaptain]]))

(use-fixtures :once with-database)

(def resolve-meme #'slacky.meme/resolve-meme-pattern)
(deftest resolve-meme-pattern-test
  (are [text meme-pattern] (= meme-pattern (resolve-meme text))

    "y u no foos?" [:NryNmg "y u no" "foos?"]
    "one does not simply photograph the photographer" [:da2i4A "one does not simply" "photograph the photographer"]

    "ceiling cat | ceiling cat | watching you" ["ceiling cat" "ceiling cat" "watching you"]
    "ceiling cat | | watching all the things" ["ceiling cat" "" "watching all the things"]

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
           (second descriptions)))))

(defn- safe-read [chan]
  (first (a/alts!! [chan (a/timeout 500)])))

(deftest register-template-test
  (let [account-id (:id (register-basic-account! *db* "foo"))]
    (cj/stubbing [memecaptain/create-template "some-template-id"]


                 (testing "can create a template"
                   (let [response (add-template *db*
                                                account-id
                                                ":template angry martin http://foo.bar/baz.jpg")]

                     (is (= [:add-template "angry martin" "http://foo.bar/baz.jpg"]
                            (safe-read response))))
                   (cj/verify-called-once-with-args memecaptain/create-template "http://foo.bar/baz.jpg")

                   (testing "use it"
                     (is (= [:some-template-id "" "bidi!!!111one"]
                            (resolve-meme-pattern *db* account-id "angry martin | | bidi!!!111one"))))

                   (testing "and delete it"
                     (is (= [:delete-template "angry martin"]
                            (safe-read (delete-template *db* account-id ":delete-template angry martin"))))

                     (is (= ["angry martin" "" "bidi!!!111one"]
                            (resolve-meme-pattern *db* account-id "angry martin | | bidi!!!111one"))))))))
