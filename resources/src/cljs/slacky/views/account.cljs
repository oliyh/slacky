(ns src.cljs.slacky.views.account
  (:require [reagent.core :as r]
            [ajax.core :refer [POST GET]]))

(def account (r/atom nil))

(defn- load-account [account-id]
  (GET (str "/api/account/" account-id)
      {:response-format :json
       :keywords? true
       :handler #(reset! account %)}))

(def account-component
  (with-meta
    (fn []
      (if @account
        [:div
         [:h1 "Your account"]
         ]
        [:h1 "Loading..."]))
    :component-will-mount load-account))
