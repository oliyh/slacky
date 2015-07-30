(ns slacky.settings)

(defn- rewrite-heroku-database-url [url]
  (condp re-find url
    #"^postgres://" ;; heroku
    (let [[_ user password host port database]
          (re-matches #"postgres://(.*):(.*)@(.*):(.*)/(.*)" url)]
      (format "jdbc:postgresql://%s:%s/%s?user=%s&password=%s"
              host port database user password))

    url))

(defn database-url []
  (or (when-let [provided-url (System/getenv "DATABASE_URL")]
        (rewrite-heroku-database-url provided-url))
      "jdbc:sqlite:memory:slacky"))

(defn web-port []
  (Integer. (or (System/getenv "PORT") 8080)))

(defn google-analytics-key []
  (System/getenv "GOOGLE_ANALYTICS_KEY"))
