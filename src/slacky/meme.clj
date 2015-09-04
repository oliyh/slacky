(ns slacky.meme
  (:require [cheshire.core :as json]
            [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]
            [slacky
             [google :as google]
             [memecaptain :as memecaptain]
             [slack :as slack]]))

(defn- url? [term]
  (re-matches #"^https?://.*$" term))

(defn- resolve-template-id [term]
  (log/debug "Resolving template for term" term)
  (cond
    (keyword? term)
    (name term)

    (string/blank? term)
    nil

    (url? term)
    (memecaptain/create-template term)

    :else
    (memecaptain/create-template (google/image-search term))))

;; from https://bitbucket.org/atlassianlabs/ac-koa-hipchat-sassy/src/1d0a72839002d9dc9f911825de73d819d7f94f5c/lib/commands/meme.js?at=master
(def ^:private meme-patterns
  [{:pattern #"^(?i)y u no (?<lower>.+)"
    :template :NryNmg
    :parser (fn [_ text-lower] ["y u no" text-lower])}

   {:pattern #"^(?i)one does not simply (?<lower>.+)"
    :template :da2i4A
    :parser (fn [_ text-lower] ["one does not simply" text-lower])}

   {:pattern #"^(?i)not sure if (?<upper>.+) or (?<lower>.+)"
    :template :CsNF8w
    :parser (fn [_ text-upper text-lower] [(str "not sure if " text-upper) (str "or " text-lower)])}

   {:pattern #"^(?i)brace yoursel(?:f|ves) (?<lower>.+)"
    :template :_I74XA
    :parser (fn [_ text-lower] ["brace yourself" text-lower])}

   {:pattern #"^(?i)success when (?<upper>.+) then (?<lower>.*)"
    :template :AbNPRQ
    :parser (fn [_ text-upper text-lower] [text-upper text-lower])}

   {:pattern #"^(?i)cry when (?<upper>.+) then (?<lower>.+)"
    :template :QZZvlg
    :parser (fn [_ text-upper text-lower] [text-upper text-lower])}

   {:pattern #"^(?i)what if i told you (?<lower>.+)"
    :template :fWle1w
    :parser (fn [_ text-lower] ["what if i told you" text-lower])}

   {:pattern #"^(?i)(?<upper>.+),? how do they work\??"
    :template :3V6rYA
    :parser (fn [_ text-upper] [text-upper "how do they work?"])}

   {:pattern #"^(?i)(?<upper>.+) all the (?<lower>.+)"
    :template :Dv99KQ
    :parser (fn [_ text-upper text-lower] [text-upper (str "all the " text-lower)])}

   {:pattern #"^(?i)(?<upper>.+),? (?:\1) everywhere"
    :template :yDcY5w
    :parser (fn [_ text-upper] [text-upper (str text-upper " everywhere")])}

   {:pattern #"<?([^>]+)>?\s?\|\s?(?<upper>.*)\s?\|\s?(?<lower>.*)\s?"
    :parser (fn [_ template-search text-upper text-lower]
              (mapv string/trim [template-search text-upper text-lower]))}
   ])


(defn- resolve-meme-pattern
  ([text] (resolve-meme-pattern nil nil text))
  ([db account-id text]
   (when-let [pattern
              (some #(when-let [matches (re-matches (:pattern %) text)]
                       (let [m (apply (:parser %) matches)]
                         (if (:template %)
                           (cons (:template %) m)
                           m))) meme-patterns)]

     (let [template (first pattern)
           template (or
                     (and account-id (string? template) (not (url? template))
                          (->
                           (jdbc/query db (sql/format {:select [:template_id]
                                                       :from [:meme_templates]
                                                       :where [:and
                                                               [:= :account_id account-id]
                                                               [:= :name (-> template string/trim string/lower-case)]]
                                                       :limit 1}))
                           first
                           :template_id
                           keyword))
                     template)]

       (into [template] (rest pattern))))))


(defn- clean-pattern [p]
  (-> p
      (.pattern)
      (string/replace "<?([^>]+)>?" "[search terms or image url]") ;; pipe
      (string/replace "(?<upper>.*)" "[upper]")
      (string/replace "(?<upper>.+)" "[upper]")
      (string/replace "(?<lower>.*)" "[lower]")
      (string/replace "(?<lower>.+)" "[lower]")
      (string/replace "(?:\\1)" "[upper]")

      (string/replace "^(?i)" "") ;; case insensitivity
      (string/replace "(?:" "(") ;; non-capturing group
      (string/replace "\\s?" " ") ;; optional space
      (string/replace "\\??" "?") ;; optional question mark
      (string/replace ",?" "") ;; optional comma
      (string/replace "\\|" "|") ;; pipe
      ))

(defn- resolve-template-registration [text]
  (re-matches #"(?i):register (.+) (https?://.+$)" text))

(defn- resolve-command [text]
  (cond
    (not (nil? (resolve-template-registration text)))
    :register-template

    (not (nil? (resolve-meme-pattern text)))
    :generate-meme

    :else
    :unknown))

(defn- generate-meme [db account-id text respond-to]
  (future
    (let [[template-search text-upper text-lower] (resolve-meme-pattern db account-id text)]
      (if-let [template-id (resolve-template-id template-search)]
        (try (let [meme-url (memecaptain/create-instance template-id text-upper text-lower)]
               (log/info "Generated meme" meme-url)
               (respond-to :success meme-url))
             (catch Exception e
               (log/error "Blew up attempting to generate meme" e)
               (respond-to :error (str "You broke me. Check my logs for details!"
                                       "\n`" text "`" ))))

        (respond-to :error
                    (str "Couldn't find a good template for the meme, try specifying a url instead"
                         "\n`" text "`")))))
  "Your meme is on its way")

(defn- register-template [db account-id text respond-to]
  (future
    (let [[_ name source-url] (resolve-template-registration text)]
      (try
        (let [template-id (memecaptain/create-template source-url)]
          (jdbc/with-db-transaction [db db]
            (jdbc/insert! db :meme_templates {:account_id account-id
                                              :name (-> name string/trim string/lower-case)
                                              :source_url source-url
                                              :template_id template-id}))
          (respond-to :success (format "Successfully created your template - refer to it as '%s'" name)))
        (catch Exception e
          (respond-to :error (format "Could not create template from %s" source-url))))))
  "Your template is being registered")

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

(def ^:private invalid-meme-syntax
  (string/join
   "\n"
   (concat ["Sorry, this is not a valid command syntax"
            "Try one the following patterns:"
            ""]
           (map :pattern (describe-meme-patterns)))))

(defn handle-request [db account-id text respond-to]
  (condp = (resolve-command text)

    :generate-meme (generate-meme db account-id text respond-to)

    :register-template (register-template db account-id text respond-to)

    :unknown (throw (IllegalArgumentException. invalid-meme-syntax))))
