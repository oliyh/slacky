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

(defn index [{:keys [google-analytics-key slack-oauth-url]}]
  (html5 {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible"
                  :content "IE=edge"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1"}]

          [:title "ZOMG"]
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
          [:div#app.container
           {:data-slack-oauth-url slack-oauth-url}
           [:div.header
            [:h1 "ZOMG"]
            [:h4 "Memes as a Service"]]
           [:div#loading-notice.jumbotron
            [:h1 "ZOMG is loading..."]
            [:p [:small "ZOMG is a modern website with modern requirements. If you cannot see anything, try upgrading your browser."]]
            [:p [:small "Stuck? Contact "
                 [:a {:href "https://twitter.com/oliyh"
                      :target "_blank"} "@oliyh"]
                 " for help."]]]]

          (include-js "/cljs/main.js")
          (google-analytics google-analytics-key)]))
