(ns slacky.meme
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [slacky.slack :as slack]))

(def memecaptain-url "http://memecaptain.com")
(def image-search-url "http://ajax.googleapis.com/ajax/services/search/images")

(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defn- poll-for-result [polling-url]
  (log/debug "Polling" polling-url)
  (loop [attempts 0]
    (let [resp (http/get polling-url {:connection-manager connection-pool :follow-redirects false})
          status (:status resp)]

      (cond

        (= 303 status)
        (get-in resp [:headers "location"])

        (< 30 attempts)
        (throw (Exception. (str "Timed out waiting")))

        (= 200 status)
        (do (log/debug "Meme not ready, sleeping")
            (Thread/sleep 1000)
            (recur (inc attempts)))

        :else
        (throw (ex-info (str "Unexpected response from " polling-url)
                        resp))))))

(defn- google-image-search [term]
  (log/info "Googling for images matching" term)
  (let [resp (http/get image-search-url
                       {:connection-manager connection-pool
                        :headers {:Content-Type "application/json"
                                  :Accept "application/json"}
                        :query-params {:q term
                                       :v 1.0
                                       :rsz 8
                                       :safe "active"}})]

    (when-let [body (and (= 200 (:status resp))
                         (:body resp))]
      (-> body (json/decode keyword) :responseData :results not-empty rand-nth :unescapedUrl))))


(defn- create-template [image-url]
  (when image-url
    (log/info "Creating template for image" image-url)
    (let [resp (http/post (str memecaptain-url "/src_images")
                          {:connection-manager connection-pool
                           :headers {:Content-Type "application/json"
                                     :Accept "application/json"}
                           :follow-redirects false
                           :body (json/encode {:url (.trim image-url)})})]

      (if-let [polling-url (and (= 202 (:status resp))
                                (not-empty (get-in resp [:headers "location"])))]

        (do (poll-for-result polling-url)
            (-> resp :body (json/decode keyword) :id))

        (throw (ex-info (str "Unexpected response from /src_images")
                        resp))))))

(defn- create-instance [template-id text-upper text-lower]
  (log/info "Generating meme based on template" template-id)
  (let [resp (http/post (str memecaptain-url "/gend_images")
                        {:connection-manager connection-pool
                         :headers {:Content-Type "application/json"
                                   :Accept "application/json"}
                         :follow-redirects false
                         :body (json/encode {:src_image_id template-id
                                             :captions_attributes
                                             [{:text text-upper
                                               :top_left_x_pct 0.05
                                               :top_left_y_pct 0
                                               :width_pct 0.9
                                               :height_pct 0.25}
                                              {:text text-lower
                                               :top_left_x_pct 0.05
                                               :top_left_y_pct 0.75
                                               :width_pct 0.9
                                               :height_pct 0.25}]})})]

    (if-let [polling-url (and (= 202 (:status resp))
                              (not-empty (get-in resp [:headers "location"])))]

      (poll-for-result polling-url)

      (throw (ex-info (str "Unexpected response")
                      resp)))))

;; from https://bitbucket.org/atlassianlabs/ac-koa-hipchat-sassy/src/1d0a72839002d9dc9f911825de73d819d7f94f5c/lib/commands/meme.js?at=master
(def ^:private known-meme-templates
  {:y-u-no               "NryNmg"
   :one-does-not-simply  "da2i4A"
   :all-the-things       "Dv99KQ"
   :not-sure-if          "CsNF8w"
   :brace-yourself       "_I74XA"
   :success              "AbNPRQ"
   :first-world-problems "QZZvlg"
   :what-if-i-told-you   "fWle1w"
   :how-do-they-work     "3V6rYA"})

(defn- resolve-template-id [term]
  (log/debug "Resolving template for term" term)
  (cond

    (contains? known-meme-templates term)
    (get known-meme-templates term)

    (string/blank? term)
    nil

    (re-matches #"^https?://.*$" term)
    (create-template term)

    :else
    (create-template (google-image-search term))))

(def command-pattern #"<?([^>]*)>?\s?\|\s?(.*)\s?\|\s?(.*)\s?")
(def not-blank? (complement string/blank?))


(defn- resolve-meme-pattern [text]
  (condp re-matches text

    #"^(?i)y u no (.*)" :>> (fn [[_ text-lower]]
                              [:y-u-no "y u no" text-lower])

    #"^(?i)one does not simply (.*)" :>> (fn [[_ text-lower]]
                                           [:one-does-not-simply "one does not simply" text-lower])

    #"(?i)not sure if (.*) or (.*)" :>> (fn [[_ text-upper text-lower]]
                                          [:not-sure-if (str "not sure if " text-upper) (str "or " text-lower)])

    #"(?i)brace yoursel(?:f|ves) (.*)" :>> (fn [[_ text-lower]]
                                             [:brace-yourself "brace yourself" text-lower])

    #"(?i)success when (.*) then (.*)" :>> (fn [[_ text-upper text-lower]]
                                             [:success text-upper text-lower])

    #"(?i)cry when (.*) then (.*)" :>> (fn [[_ text-upper text-lower]]
                                         [:first-world-problems text-upper text-lower])

    #"(?i)what if i told you (.*)" :>> (fn [[_ text-lower]]
                                         [:what-if-i-told-you "what if i told you" text-lower])

    #"(?i)(.*) how do they work\??" :>> (fn [[_ text-upper]]
                                          [:how-do-they-work text-upper "how do they work?"])

    #"(?i)(.*) all the (.*)" :>> (fn [[_ text-upper text-lower]]
                                   [:all-the-things text-upper (str "all the " text-lower)])

    (let [[_ template-search text-upper text-lower] (map #(.trim %) (re-matches command-pattern text))]
      (when (and (not-blank? template-search)
                 (some not-blank? [text-upper text-lower]))
        [template-search text-upper text-lower]))))

(defn valid-command? [text]
  (not (nil? (resolve-meme-pattern text))))

(defn generate-meme [text respond-to]
  (let [[template-search text-upper text-lower]
        (resolve-meme-pattern text)]

    (if-let [template-id (resolve-template-id template-search)]
      (try (let [meme-url (create-instance template-id text-upper text-lower)]
             (log/info "Generated meme" meme-url)
             (respond-to :success meme-url))
           (catch Exception e
             (log/error "Blew up attempting to generate meme" e)
             (respond-to :error (str "You broke me. Check my logs for details!"
                                     "\n`" text "`" ))))

      (respond-to :error
                  (str "Couldn't find a good template for the meme, try specifying a url instead"
                       "\n`" text "`")))))
