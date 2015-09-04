(ns slacky.google
  (:require [clj-http
             [client :as http]
             [conn-mgr :refer [make-reusable-conn-manager]]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(def image-search-url "https://ajax.googleapis.com/ajax/services/search/images")
(def connection-pool (make-reusable-conn-manager {:timeout 10 :threads 4 :default-per-route 4}))

(defn image-search [term]
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
