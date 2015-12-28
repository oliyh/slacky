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

(defn- example [img-src command]
  [:div.col-xs-12.col-md-4
   [:div.example {:data-command command}
    [:img.img-thumbnail {:src img-src}]
    [:code command]]])

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

          (include-css "css/bootstrap.min.css"
                       "https://fonts.googleapis.com/css?family=Quicksand:300"
                       "css/avgrund.css"
                       "css/typeahead.css"
                       "css/main.css")
          [:link {:rel "chrome-webstore-item"
                  :href "https://chrome.google.com/webstore/detail/nikjbdhcbfledkekdacecmegploaelpe"}]]
         [:body

          [:a.hidden-xs {:href "https://github.com/oliyh/slacky"}
           [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
                  :src "https://camo.githubusercontent.com/652c5b9acfaddf3a9c326fa6bde407b87f7be0f4/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f6f72616e67655f6666373630302e706e67"
                  :alt "Fork me on GitHub"
                  :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_orange_ff7600.png"}]]

          [:div#app.container.avgrund-parent
           {:data-slack-oauth-url slack-oauth-url}

           [:div.header
            [:h1 "Slacky"]
            [:h4 "Memes as a Service"]]

           [:div.row
            [:div.col-xs-10.col-xs-offset-1

             [:div#footer.col-xs-12
              [:h4 "Help"]
              [:small "Contact "
               [:a {:href "https://twitter.com/oliyh"
                    :target "_blank"} "@oliyh"]
               " on Twitter"]
              [:h4 "Privacy"]
              [:small "Slacky stores a randomly generated token to identify your account, allowing
you to take advantage of various features."]]]]]

          [:div#demo-meme-popup.avgrund-popup
           [:div
            [:div.modal-header
              [:button.close {:type "button"
                              :onClick "Avgrund.hide();"}
               [:span {:aria-hidden "true"} "&times;"]]
             [:h4.modal-title "Brace yourself..."]]
            [:div.modal-body
             [:img#demo-meme.center-block {:src ""}]]
            [:div.modal-footer
              [:button.btn.btn-default {:type "button"
                                        :onClick "Avgrund.hide();"}
               "Close"]]]]

          [:div#add-to-slack-success.avgrund-popup
           [:div
            [:div.modal-header
              [:button.close {:type "button"
                              :onClick "Avgrund.hide();"}
               [:span {:aria-hidden "true"} "&times;"]]
             [:h4.modal-title "Success!"]]
            [:div.modal-body
             [:p "Start using in Slack right away by typing "
              [:code "/meme"]
              " in any channel"]
             [:p "Stuck? Type "
              [:code "/meme :help"]
              " or try the demos on this page"]]
            [:div.modal-footer
              [:button.btn.btn-default {:type "button"
                                        :onClick "Avgrund.hide();"}
               "Close"]]]]

          [:div#add-to-slack-failure.avgrund-popup
           [:div
            [:div.modal-header
              [:button.close {:type "button"
                              :onClick "Avgrund.hide();"}
               [:span {:aria-hidden "true"} "&times;"]]
             [:h4.modal-title "Failure"]]
            [:div.modal-body
             [:p "Something went wrong. Contact me at @oliyh on Twitter if you need help!"]]
            [:div.modal-footer
              [:button.btn.btn-default {:type "button"
                                        :onClick "Avgrund.hide();"}
               "Close"]]]]

          [:div#add-to-slack-denied.avgrund-popup
           [:div
            [:div.modal-header
              [:button.close {:type "button"
                              :onClick "Avgrund.hide();"}
               [:span {:aria-hidden "true"} "&times;"]]
             [:h4.modal-title "Denied"]]
            [:div.modal-body
             [:p "You will have to authorise Slacky to be able to use it in Slack."]]
            [:div.modal-footer
              [:button.btn.btn-default {:type "button"
                                        :onClick "Avgrund.hide();"}
               "Close"]]]]

          (include-js ;;"https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"
                      ;;"js/bootstrap.min.js"
                      ;;"js/typeahead.jquery.js"
                      "js/avgrund.js"
                      "/cljs/main.js")
          (google-analytics google-analytics-key)]))
