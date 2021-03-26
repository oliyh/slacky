(ns slacky.bing
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [net.cgrand.enlive-html :as html]
            [cheshire.core :as json]))

(def image-search-url "https://www.bing.com/images/search")
(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defn- search-params [term]
  (if (re-find #":anim" term)
    {:q (-> (string/replace term #":anim" "")
            (string/trim))
     :qft "+filterui:photo-animatedgif"}
    {:q (string/trim term)}))

(defn image-search [term]
  (log/info "Binging for images matching" term)
  (let [resp (http/get image-search-url
                       {:connection-manager connection-pool
                        :headers {:Content-Type "application/json"
                                  :Accept "application/json"}
                        :query-params (search-params term)})]

    (when-let [body (and (= 200 (:status resp))
                         (:body resp))]
      (some->> (html/select (html/html-snippet body) [:div.imgpt :a.iusc])
               not-empty
               (take 5)
               rand-nth
               :attrs
               :m
               (#(json/decode % keyword))
               :murl))))
