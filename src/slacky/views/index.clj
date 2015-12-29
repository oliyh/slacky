(ns slacky.views.index
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [cheshire.core :as json]))

(defn- google-analytics [google-analytics-key]
  (when google-analytics-key
    [:script
     "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){"
     "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),"
     "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)"
     "})(window,document,'script','//www.google-analytics.com/analytics.js','ga');"

     "ga('create', '" google-analytics-key "', 'auto');"
     "ga('send', 'pageview');"]))

(defn index [{:keys [google-analytics-key meme-descriptions slack-oauth-url]}]
  (html5 {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible"
                  :content "IE=edge"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1"}]

          [:title "Slacky"]
          [:link {:id "favicon"
                  :rel "shortcut icon"
                  :href "/images/slacky-64.png"
                  :sizes "16x16 32x32 48x48"
                  :type "image/png"}]

          (include-css "/css/bootstrap.min.css"
                       "https://fonts.googleapis.com/css?family=Quicksand:300"
                       "/css/typeahead.css"
                       "/css/main.css")
          [:link {:rel "chrome-webstore-item"
                  :href "https://chrome.google.com/webstore/detail/nikjbdhcbfledkekdacecmegploaelpe"}]]
         [:body

          [:a.hidden-xs {:href "https://github.com/oliyh/slacky"}
           [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
                  :src "https://camo.githubusercontent.com/652c5b9acfaddf3a9c326fa6bde407b87f7be0f4/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f6f72616e67655f6666373630302e706e67"
                  :alt "Fork me on GitHub"
                  :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_orange_ff7600.png"}]]

          [:div#app.container.avgrund-parent
           {:data-slack-oauth-url slack-oauth-url}]

          (include-js ;;"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"
                      ;;"js/bootstrap.min.js"
                      ;;"js/typeahead.jquery.js"
                      ;;"js/avgrund.js"
                      "/cljs/main.js")
          (google-analytics google-analytics-key)]))
