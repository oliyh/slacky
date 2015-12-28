(ns slacky.views.integrations
  (:require [secretary.core :as secretary]))

(defn- avgrund-modal [id title content]
  (let [hide-modal #(.Avgrund.hide js/window)
        aria-label (str id "-title")]
    [:div.avgrund-popup {:id id
                         :tabIndex "-1"
                         :role "dialog"
                         :aria-labelledby aria-label}
     [:div {:role "document"}
      [:div
       [:div.modal-header
        [:button.close {:type "button"
                        :on-click hide-modal}
         [:span {:aria-hidden "true"} "×"]]
        [:h4.modal-title
         {:id aria-label}
         title]]
       [:div.modal-body
        content]
       [:div.modal-footer
        [:button.btn.btn-default {:type "button"
                                  :on-click hide-modal}
         "Close"]]]]]))

(defn modals [slack-oauth-url]
  [avgrund-modal
   "upgrade-slack-modal"
   "Upgrade Slack"
   [:div
    [:h3 "1. Use Slacky once more"]
    [:p "Type "
     [:code "/meme :help"]
     " in any channel in Slack. This will prepare your account to be upgraded."]
    [:h3 "2. Authenticate the app"]
    [:p "Press "
     [:a#slack-upgrade {:href slack-oauth-url
                        :target "_blank"}
      [:img {:alt "Add to Slack"
             :height 40
             :width 139
             :src "https://platform.slack-edge.com/img/add_to_slack.png"
             :srcSet "https://platform.slack-edge.com/img/add_to_slack.png 1x, https://platform.slack-edge.com/img/add_to_slack@2x.png 2x"}]]]]])

(defn component [slack-oauth-url]
  [:div.jumbotron
   [:div
    [:a {:name "slack"}]
    [:div.leader
     [:div#memes-in-slack
      [:span.h1 "Memes in"]
      [:img {:src "/images/slack-logo.png"
             :alt "Slack"}]]
     [:a#slack-install {:href slack-oauth-url
                        :target "_blank"}
      [:img {:alt "Add to Slack"
             :height 80
             :width 278
             :src "https://platform.slack-edge.com/img/add_to_slack@2x.png"}]]

     [:p
      [:a {:href "#upgrade-slack-modal"
           :on-click #(.Avgrund.show js/window "#upgrade-slack-modal")}
       "Already installed Slacky the old way?"]]]]

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
       [:p "Click here to add to Firefox"]]]]]])
