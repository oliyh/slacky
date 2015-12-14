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
         [:body.avgrund-parent

          [:a.hidden-xs {:href "https://github.com/oliyh/slacky"}
           [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
                  :src "https://camo.githubusercontent.com/652c5b9acfaddf3a9c326fa6bde407b87f7be0f4/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f6f72616e67655f6666373630302e706e67"
                  :alt "Fork me on GitHub"
                  :data-canonical-src "https://s3.amazonaws.com/github/ribbons/forkme_right_orange_ff7600.png"}]]

          [:div.avgrund-cover]

          [:div.container.avgrund-contents
           [:div.header
            [:h1 "Slacky"]
            [:h4 "Memes as a Service"]]

           [:div.row
            [:div.col-xs-10.col-xs-offset-1


             [:div.jumbotron
              [:a {:name "demo"}]
              [:div.leader
               [:h1 "How can haz meme?"]]

              [:div.row
               (example "/images/pai-mei-approves.png" "pai mei | pai mei | approves")
               (example "/images/slacky-wins.png" "https://goo.gl/h9eUDM | slacky | wins")
               (example "/images/all-the-memes.png" "create all the memes!")]

              [:div.row
               [:div.col-xs-12
                [:form#demo.form-horizontal
                 [:div.col-xs-12.col-md-11
                  [:div.form-group.form-group-lg
                   [:div.input-group
                    [:div.input-group-addon "/meme"]
                    [:input#demo-text.form-control {:type "text"
                                                    :placeholder "search term or url | upper text | lower text"}]
                    [:script {:type "text/javascript"}
                     "patterns = " (json/encode meme-descriptions)]]]]

                 [:div.col-xs-12.col-md-1
                  [:div.form-group.form-group-lg
                   [:button.btn.btn-success.btn-lg {:type "submit"}
                    "Try!"]]]]]]]


             [:div.jumbotron
              [:div
               [:a {:name "slack" :href slack-oauth-url}]
               [:div.leader
                [:div#memes-in-slack
                 [:span.h1 "Memes in"]
                 [:img {:src "/images/slack-logo.png"
                        :alt "Slack"}]]
                [:p "Create your slash command"
                 "&nbsp;"
                 [:a {:href "#guide"
                      :onClick "Avgrund.show('#guide-modal');"}
                  "by following these simple steps"]
                 "."]]]

              [:div.row
               [:div.col-xs-6
                [:a {:name "chrome"}]
                [:div.leader
                 [:a#chrome-install {:href "#chrome"}
                  [:img {:src "/images/chrome-logo.png"
                         :alt "Chrome"}]
                  [:p "Click here to add to Chrome"]]]]

               [:div.col-xs-6
                [:a {:name "firefox"}]
                [:div.leader
                 [:a#firefox-install {:href "https://addons.mozilla.org/en-US/firefox/addon/slacky/"
                                      :target "_blank"}
                  [:img {:src "/images/firefox-logo.png"
                         :alt "Firefox"}]
                  [:p "Click here to add to Firefox"]]]]]]]]]

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

          [:div#guide-modal.avgrund-popup {:tabindex "-1"
                                           :role "dialog"
                                           :aria-labelledby "myModalLabel"}
           [:div {:role "document"}
            [:div
             [:div.modal-header
              [:button.close {:type "button"
                              :onClick "Avgrund.hide();"}
               [:span {:aria-hidden "true"} "&times;"]]
              [:h4#myModalLabel.modal-title "Guide"]]
             [:div.modal-body
              [:h3 "1. Create a Slash command"]
              [:p "Create a "
               [:a {:href "https://my.slack.com/services/new/slash-commands/"
                    :target "_blank"}
                "Slash command"]
               " with the following attributes"]
              [:p
               [:strong "Command"] ": " [:code "/meme"] [:br]
               [:strong "URL"] ": " [:code "https://slacky-server.herokuapp.com:443/api/slack/meme"] [:br]
               [:strong "Method"] ": " [:code "POST"] [:br]
               [:strong "Autocomplete description"] ": " [:code "Create a meme"] [:br]
               [:strong "Autocomplete usage hint"] ": " [:code "image url or search term | upper text | lower text"] [:br]]

              [:h3 "2. Create all the memes!"]
              [:p "Type "
               [:code "/meme"]
               " into your Slack and never look back! See the examples on this page."]]
             [:div.modal-footer
              [:button.btn.btn-default {:type "button"
                                        :onClick "Avgrund.hide();"}
               "Close"]]]]]

          (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"
                      "js/bootstrap.min.js"
                      "js/typeahead.jquery.js"
                      "js/avgrund.js"
                      "js/home.js")
          (google-analytics google-analytics-key)]))
