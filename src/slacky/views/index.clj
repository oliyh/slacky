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

(defn index [{:keys [google-analytics-key meme-descriptions]}]
  (html5 {:lang "en"}
         [:head
          [:meta {:charset "utf-8"}]
          [:meta {:http-equiv "X-UA-Compatible"
                  :content "IE=edge"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1"}]

          [:title "Slacky"]

          (include-css "css/bootstrap.min.css"
                       "https://fonts.googleapis.com/css?family=Quicksand:300"
                       "css/avgrund.css"
                       "css/typeahead.css"
                       "css/main.css")]
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
              [:div.leader
               [:div#memes-in-slack
                [:span.h1 "Memes in"]
                [:img {:src "/images/slack-logo.png"
                       :alt "Slack"}]]
               [:p "Register your slash command token with your incoming webhook url below."
                "&nbsp;"
                [:a {:href "#guide"
                     :onClick "Avgrund.show('#guide-modal');"}
                 "Need help?"]]]

              [:form#new-account.form-horizontal
               [:div.form-group.form-group-lg
                [:label.col-sm-4.control-label {:for "token"}
                 "Slash command token"]
                [:div.col-sm-8
                 [:input#token.form-control {:type "text"
                                             :placeholder ""}]]]
               [:div.form-group.form-group-lg
                [:label.col-sm-4.control-label {:for "key"}
                 "Incoming webhook url"]
                [:div.col-sm-8
                 [:input#key.form-control {:type "text"
                                           :placeholder "https://hooks.slack.com/services/..."}]]]

               [:div.form-group.form-group-lg
                [:label.col-sm-offset-4.col-sm-8
                 [:button.btn.btn-success.btn-lg {:type "submit"}
                  "Let's go!"]
                 [:div#success-message.alert.alert-success
                  [:span.glyphicon.glyphicon-ok {:aria-hidden "true"}]
                  "&nbsp;"
                  [:strong "Success!"]
                  " Let the memes begin!"]
                 [:div#failure-message.alert.alert-danger
                  [:span.glyphicon.glyphicon-fire {:aria-hidden "true"}]
                  "&nbsp;"
                  [:strong "Oh noes!"]
                  " Something broke! You can try again, or give up..."]]]]]]]]

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
              [:p "Take note of the "
               [:strong "token"]
               ". You will use this to register your account."]

              [:h3 "2. Create an incoming webhook"]
              [:p "Create an "
               [:a {:href "https://my.slack.com/services/new/incoming-webhook/"
                    :target "_blank"}
                "incoming webhook"]
               "."]
              [:p "Take note of the "
               [:strong "webhook url"]
               ". This is the second piece of information needed to register your account."]

              [:h3 "3. Register your account"]
              [:p "Fill in and submit the form using the information you have collected."]

              [:h3 "4. Create all the memes!"]
              [:p "Type "
               [:code "/meme"]
               " into your Slack and never look back! See the examples below."]]
             [:div.modal-footer
              [:button.btn.btn-default {:type "button"
                                        :onClick "Avgrund.hide();"}
               "Close"]]]]]

          (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"
                      "js/bootstrap.min.js"
                      "js/typeahead.jquery.js"
                      "js/avgrund.js"
                      "js/home.js")
          (google-analytics google-analytics-key)])
  )
