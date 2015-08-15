(ns slacky.meme
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [slacky.slack :as slack]))

(def memecaptain-url "http://memecaptain.com")
(def image-search-url "https://ajax.googleapis.com/ajax/services/search/images")

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

(defn- resolve-template-id [term]
  (log/debug "Resolving template for term" term)
  (cond
    (keyword? term)
    (name term)

    (string/blank? term)
    nil

    (re-matches #"^https?://.*$" term)
    (create-template term)

    :else
    (create-template (google-image-search term))))

;; from https://bitbucket.org/atlassianlabs/ac-koa-hipchat-sassy/src/1d0a72839002d9dc9f911825de73d819d7f94f5c/lib/commands/meme.js?at=master
(def ^:private meme-patterns
  [{:pattern #"^(?i)y u no (?<lower>.*)"
    :template :NryNmg
    :parser (fn [_ text-lower] ["y u no" text-lower])}

   {:pattern #"^(?i)one does not simply (?<lower>.*)"
    :template :da2i4A
    :parser (fn [_ text-lower] ["one does not simply" text-lower])}

   {:pattern #"^(?i)not sure if (?<upper>.*) or (?<lower>.*)"
    :template :CsNF8w
    :parser (fn [_ text-upper text-lower] [(str "not sure if " text-upper) (str "or " text-lower)])}

   {:pattern #"^(?i)brace yoursel(?:f|ves) (?<lower>.*)"
    :template :_I74XA
    :parser (fn [_ text-lower] ["brace yourself" text-lower])}

   {:pattern #"^(?i)success when (?<upper>.*) then (?<lower>.*)"
    :template :AbNPRQ
    :parser (fn [_ text-upper text-lower] [text-upper text-lower])}

   {:pattern #"^(?i)cry when (?<upper>.*) then (?<lower>.*)"
    :template :QZZvlg
    :parser (fn [_ text-upper text-lower] [text-upper text-lower])}

   {:pattern #"^(?i)what if i told you (?<lower>.*)"
    :template :fWle1w
    :parser (fn [_ text-lower] ["what if i told you" text-lower])}

   {:pattern #"^(?i)(?<upper>.*) how do they work\??"
    :template :3V6rYA
    :parser (fn [_ text-upper] [text-upper "how do they work?"])}

   {:pattern #"^(?i)(?<upper>.*) all the (?<lower>.*)"
    :template :Dv99KQ
    :parser (fn [_ text-upper text-lower] [text-upper (str "all the " text-lower)])}

   {:pattern #"<?([^>]+)>?\s?\|\s?(?<upper>.*)\s?\|\s?(?<lower>.*)\s?"
    :parser (fn [_ template-search text-upper text-lower]
              (mapv string/trim [template-search text-upper text-lower]))}
   ])


(defn- resolve-meme-pattern [text]
  (some #(when-let [matches (re-matches (:pattern %) text)]
           (let [m (apply (:parser %) matches)]
             (if (:template %)
               (cons (:template %) m)
               m))) meme-patterns))


(defn- clean-pattern [p]
  (-> p
      (.pattern)
      (string/replace "<?([^>]+)>?\\s?" "[search terms or image url]") ;; pipe
      (string/replace "(?<upper>.*)" "[upper]")
      (string/replace "(?<lower>.*)" "[lower]")

      (string/replace "^(?i)" "") ;; case insensitivity
      (string/replace "(?:" "(") ;; non-capturing group
      (string/replace "\\s?" "") ;; optional space
      (string/replace "\\??" "?") ;; optional question mark
      (string/replace "\\|" "|") ;; pipe
      ))

(defn describe-meme-patterns []
  (mapv
     #(cond-> %
        :always
        (update :pattern clean-pattern)

        (:template %)
        (update :template (fn [t] (str "http://i.memecaptain.com/src_images/" (name t) ".jpg")))

        :always
        (dissoc :parser))
   meme-patterns))


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
