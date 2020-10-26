(ns slacky.meme
  (:require [cheshire.core :as json]
            [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [clojure.core.async :as a]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]
            [slacky
             [bing :as bing]
             [memecaptain :as memecaptain]
             [slack :as slack]
             [templates :as templates]]
            [slacky.settings :as settings]))

(defn- url? [term]
  (re-matches #"^https?://.*$" term))

(defn- resolve-template-url [term]
  (log/debug "Resolving template for term" term)
  (cond
    (string/blank? term)
    nil

    (url? term)
    term

    :else
    (bing/image-search term)))

;; from https://bitbucket.org/atlassianlabs/ac-koa-hipchat-sassy/src/1d0a72839002d9dc9f911825de73d819d7f94f5c/lib/commands/meme.js?at=master
(def ^:private meme-patterns
  [{:pattern #"<?([^>]+)>?\s?\|\s?(?<upper>.*)\s?\|\s?(?<lower>.*)\s?"
    :parser (fn [_ template-search text-upper text-lower]
              (mapv string/trim [template-search text-upper text-lower]))}

   {:pattern #"^(?i)y u no (?<lower>.+)"
    :template "https://imgflip.com/s/meme/Y-U-No.jpg"
    :parser (fn [_ text-lower] ["y u no" text-lower])}

   {:pattern #"^(?i)one does not simply (?<lower>.+)"
    :template "https://imgflip.com/s/meme/One-Does-Not-Simply.jpg"
    :parser (fn [_ text-lower] ["one does not simply" text-lower])}

   {:pattern #"^(?i)not sure if (?<upper>.+) or (?<lower>.+)"
    :template "https://imgflip.com/s/meme/Futurama-Fry.jpg"
    :parser (fn [_ text-upper text-lower] [(str "not sure if " text-upper) (str "or " text-lower)])}

   {:pattern #"^(?i)brace yoursel(?:f|ves) (?<lower>.+)"
    :template "https://imgflip.com/s/meme/Brace-Yourselves-X-is-Coming.jpg"
    :parser (fn [_ text-lower] ["brace yourself" text-lower])}

   {:pattern #"^(?i)success when (?<upper>.+) then (?<lower>.*)"
    :template "https://imgflip.com/s/meme/Success-Kid.jpg"
    :parser (fn [_ text-upper text-lower] [text-upper text-lower])}

   {:pattern #"^(?i)cry when (?<upper>.+) then (?<lower>.+)"
    :template "https://imgflip.com/s/meme/First-World-Problems.jpg"
    :parser (fn [_ text-upper text-lower] [text-upper text-lower])}

   {:pattern #"^(?i)what if i told you (?<lower>.+)"
    :template "https://imgflip.com/s/meme/Matrix-Morpheus.jpg"
    :parser (fn [_ text-lower] ["what if i told you" text-lower])}

   {:pattern #"^(?i)(?<upper>.+),? how do they work\??"
    :template "https://i.imgflip.com/18jot2.jpg"
    :parser (fn [_ text-upper] [text-upper "how do they work?"])}

   {:pattern #"^(?i)(?<upper>.+) all the (?<lower>.+)"
    :template "https://imgflip.com/s/meme/X-All-The-Y.jpg"
    :parser (fn [_ text-upper text-lower] [text-upper (str "all the " text-lower)])}

   {:pattern #"^(?i)(?<upper>.+),? (?:\1) everywhere"
    :template "https://imgflip.com/s/meme/X-Everywhere.jpg"
    :parser (fn [_ text-upper] [text-upper (str text-upper " everywhere")])}

   {:pattern #"^(?i)good news,? everyone[.:;,]? (?<lower>.+)"
    :template "https://i.imgflip.com/73sbv.jpg"
    :parser (fn [_ text-lower] ["good news, everyone" text-lower])}

   {:pattern #"^(?i)the (?<upper>.+) is too damn high!?"
    :template "https://imgflip.com/s/meme/Too-Damn-High.jpg"
    :parser (fn [_ text-upper] [(str "the " text-upper) "is too damn high"])}

   {:pattern #"^(?i)(?<upper>.+) why not zoidberg\??"
    :template "https://imgflip.com/s/meme/Futurama-Zoidberg.jpg"
    :parser (fn [_ text-upper] [text-upper "why not zoidberg?"])}])


(defn resolve-meme-pattern
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
                          (templates/lookup db account-id template))
                     template)]

       (into [template] (rest pattern))))))

(defn generate-meme [db account-id text]
  (let [response-chan (a/chan)]
    (a/thread
      (when-let [[template-search text-upper text-lower] (resolve-meme-pattern db account-id text)]
        (if-let [template-url (resolve-template-url template-search)]
          (try (let [meme-url (str (settings/server-dns) "/" (memecaptain/create-direct template-url text-upper text-lower))]
                 (log/info "Generated meme" meme-url "from command" text)
                 (a/>!! response-chan [:meme meme-url]))
               (catch Exception e
                 (log/error "Blew up attempting to generate meme" e)
                 (a/>!! response-chan [:error (str "You broke me. Check my logs for details!"
                                                   "\n`" text "`" )])))

          (a/>!! response-chan [:error
                                (str "Couldn't find a good template for the meme, try specifying a url instead"
                                     "\n`" text "`")])))
      (a/close! response-chan))
    response-chan))

(defn resolve-template-addition [text]
  (re-matches #"(?i):template (.+) (https?://.+$)" text))

(defn add-template [db account-id text]
  (let [response-chan (a/chan)]
    (a/thread
      (let [[_ template-name source-url] (resolve-template-addition text)]
        (try
          (templates/persist! db account-id template-name source-url)
          (a/>!! response-chan [:add-template template-name source-url])
          (catch Exception e
            (a/>!! response-chan [:error (format "Could not create template from %s" source-url)]))))
      (a/close! response-chan))
    response-chan))

(defn resolve-template-deletion [text]
  (re-matches #"(?i):delete-template (.+)" text))

(resolve-template-deletion ":delete-template foo bar")

(defn delete-template [db account-id text]
  (let [response-chan (a/chan)]
    (a/thread
      (let [[_ template-name] (resolve-template-deletion text)]
        (try
          (templates/delete! db account-id template-name)
          (a/>!! response-chan [:delete-template template-name])
          (catch Exception e
            (println e)
            (a/>!! response-chan [:error (format "Could not delete template \"%s\"" template-name)]))))
      (a/close! response-chan))
    response-chan))

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
      (string/replace "!?" "") ;; optional exclamation mark
      (string/replace ",?" "") ;; optional comma
      (string/replace "\\|" "|") ;; pipe
      (string/replace "[.:;,]?" "") ;; optional punctuation
      ))

(defn describe-meme-patterns []
  (mapv
   #(cond-> %
      :always
      (update :pattern clean-pattern)

      :always
      (dissoc :parser))
   meme-patterns))

(defn generate-help [db account-id]
  (string/join "\n"
               (concat
                ["Create a meme using one of the following patterns:"]
                (map :pattern (describe-meme-patterns))
                [""
                 "Create a template to use in memes:"
                 "/meme :template [name of template] https://cats.com/cat.jpg"]
                [""
                 "Delete a template"
                 "/meme :delete-template [name of template]"]
                (when-let [templates (and account-id
                                          (not-empty (templates/list db account-id)))]
                  (cons "\nCustom templates:" (map :name templates))))))
