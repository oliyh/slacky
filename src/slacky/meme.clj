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
             [templates :as templates]]))

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
    (memecaptain/create-template (bing/image-search term))))

;; from https://bitbucket.org/atlassianlabs/ac-koa-hipchat-sassy/src/1d0a72839002d9dc9f911825de73d819d7f94f5c/lib/commands/meme.js?at=master
(def ^:private meme-patterns
  [{:pattern #"<?([^>]+)>?\s?\|\s?(?<upper>.*)\s?\|\s?(?<lower>.*)\s?"
    :parser (fn [_ template-search text-upper text-lower]
              (mapv string/trim [template-search text-upper text-lower]))}

   {:pattern #"^(?i)y u no (?<lower>.+)"
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
  
   {:pattern #"^(?i)good news,? everyone[.:;,]? (?<lower>.+)"
    :template :7SthVg
    :parser (fn [_ text-lower] ["good news, everyone" text-lower])}
  
   {:pattern #"^(?i)the (?<upper>.+) is too damn high"
    :template :RCkv6Q
    :parser (fn [_ text-upper] ["the " text-upper "is too damn high"])}  

   {:pattern #"^(?i)(?<upper>.+) why not zoidberg\??"
    :template :kzsGfQ
    :parser (fn [_ text-upper] [text-upper "why not zoidberg?"])}
   ])


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
        (if-let [template-id (resolve-template-id template-search)]
          (try (let [meme-url (memecaptain/create-instance template-id text-upper text-lower)]
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
      (let [[_ name source-url] (resolve-template-addition text)]
        (try
          (let [template-id (memecaptain/create-template source-url)]
            (templates/persist! db account-id name source-url template-id)
            (a/>!! response-chan [:add-template name source-url]))
          (catch Exception e
            (a/>!! response-chan [:error (format "Could not create template from %s" source-url)]))))
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
      (string/replace ",?" "") ;; optional comma
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

(defn generate-help [db account-id]
  (string/join "\n"
               (concat
                ["Create a meme using one of the following patterns:"]
                (map :pattern (describe-meme-patterns))
                [""
                 "Create a template to use in memes:"
                 "/meme :template [name of template] https://cats.com/cat.jpg"]
                (when-let [templates (and account-id
                                          (not-empty (templates/list db account-id)))]
                  (cons "\nCustom templates:" (map :name templates))))))
