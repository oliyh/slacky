(ns slacky.views.index
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn- google-analytics [google-analytics-key]
  (when google-analytics-key
    [:div
     [:script {:async true
               :src (format "https://www.googletagmanager.com/gtag/js?id=%s" google-analytics-key)}]
     [:script
      (format "window.dataLayer = window.dataLayer || [];
                     function gtag(){dataLayer.push(arguments);}
                     gtag('js', new Date());
                     gtag('config', '%s');"
              google-analytics-key)]]))

(defn index [{:keys [google-analytics-key slack-oauth-url app-name]}]
  (html5 {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible"
                  :content "IE=edge"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1"}]

          [:title app-name]
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

          [:div#app.container
           {:data-slack-oauth-url slack-oauth-url
            :data-app-name app-name}
           [:div.header
            [:h1 app-name]
            [:h4 "Memes as a Service"]]
           [:div#loading-notice.jumbotron
            [:h1 (format "%s is loading..." app-name)]
            [:p [:small (format "%s is a modern website with modern requirements. If you cannot see anything, try upgrading your browser."
                                app-name)]]
            [:p [:small "Stuck? Contact "
                 [:a {:href "https://twitter.com/oliyh"
                      :target "_blank"} "@oliyh"]
                 " for help."]]]]

          (include-js "/cljs/main.js")
          (google-analytics google-analytics-key)]))
