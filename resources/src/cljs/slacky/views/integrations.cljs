(ns slacky.views.integrations
  (:require [slacky.nav :as nav]
            [slacky.analytics :refer [event!]]))

(defn- slack-success []
  [:div
   [:p "Start using in Slack right away by typing "
    [:code "/meme"]
    " in any channel"]
   [:p "Stuck? Type "
    [:code "/meme :help"]
    " or try the demos on this page"]])

(defn- slack-failure []
  [:div
   [:p "Something went wrong. Contact me at @oliyh on Twitter if you need help!"]])

(defn- slack-denied []
  [:div
   [:p "You will have to authorise Slacky to be able to use it in Slack."]])

(defn slack-upgrade [slack-oauth-url]
  [:div
   [:h3 "1. Use Slacky once more"]
   [:p "Type "
    [:code "/meme :help"]
    " in any channel in Slack. This will prepare your account to be upgraded."]
   [:h3 "2. Authenticate the app"]
   [:p "Press "
    [:a {:href slack-oauth-url
         :on-click #(do (event! "slack-upgrade")
                        (event! "slack-install"))
         :target "_blank"}
     [:img {:alt "Add to Slack"
            :height 40
            :width 139
            :src "https://platform.slack-edge.com/img/add_to_slack.png"
            :srcSet "https://platform.slack-edge.com/img/add_to_slack.png 1x, https://platform.slack-edge.com/img/add_to_slack@2x.png 2x"}]]]])

(defn component [slack-oauth-url]
  [:div.jumbotron
   [:div
    [:div.leader
     [:div#memes-in-slack
      [:span.h1 "Memes in"]
      [:img {:src "/images/slack-logo.png"
             :alt "Slack"}]]
     [:a#slack {:href slack-oauth-url
                :target "_blank"
                :on-click #(event! "slack-install")}
      [:img {:alt "Add to Slack"
             :height 80
             :width 278
             :src "https://platform.slack-edge.com/img/add_to_slack@2x.png"}]]

     [:p
      [:a {:href "/slack/upgrade"
           :on-click (nav/nav! "/slack/upgrade")}
       "Already installed Slacky the old way?"]]]]

   [:div.row
    [:div.col-xs-12.col-md-6
     [:div.leader
      [:a#chrome {:href "#chrome"
                  :on-click #(do (event! "chrome-install")
                                 (when (and js/window.chrome.webstore (not js/window.chrome.app.isInstalled))
                                   (.install js/window.chrome.webstore js/undefined js/undefined (fn [error result-code]
                                                                                                   (js/console.log "Failure installing Chrome extension: " result-code " - " error)))))}
       [:img {:src "/images/chrome-logo.png"
              :alt "Chrome"}]
       [:p "Click here to add to Chrome"]]]]

    [:div.col-xs-12.col-md-6
     [:div.leader
      [:a#firefox {:href "https://addons.mozilla.org/en-US/firefox/addon/slacky/"
                   :target "_blank"
                   :on-click #(event! "firefox-install")}
       [:img {:src "/images/firefox-logo.png"
              :alt "Firefox"}]
       [:p "Click here to add to Firefox"]]]]]])
