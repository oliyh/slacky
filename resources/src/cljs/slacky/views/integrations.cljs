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
   [:p "You will have to authorise this service to be able to use it in Slack."]])

(defn slack-upgrade [slack-oauth-url]
  [:div
   [:h3 "1. Use once more"]
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
   [:div.row
    [:div.col-xs-12.col-md-6
     [:div.leader
      [:div
       [:span.h1 "Get it!"]]

      [:a#slack.integration-button
       {:href slack-oauth-url
        :target "_blank"
        :on-click #(event! "slack-install")}
       " "
       [:img {:alt "Add to Slack"
              :height 80
              :width 278
              :src "https://platform.slack-edge.com/img/add_to_slack@2x.png"}]]

      [:p
       [:small [:a {:href "/slack/upgrade"
                    :on-click (nav/nav! "/slack/upgrade")}
                "Already installed on Slack the old way?"]]]]]

    [:div.col-xs-12.col-md-6 {:style {:border "none" :text-align "center"}}
     [:iframe {:src "https://player.vimeo.com/video/138360289?title=0&byline=0&portrait=0"
               :width "100%"
               :height 230
               :frameBorder 0
               :webkitallowfullscreen true
               :mozallowfullscreen true
               :allowFullScreen true}]]]

   [:div.row
    [:div.col-xs-12.col-md-6
     [:div.leader
      [:a#chrome.integration-button
       {:href "#chrome"
        :on-click #(do (event! "chrome-install")
                       (when (and js/window.chrome.webstore (not js/window.chrome.app.isInstalled))
                         (.install js/window.chrome.webstore js/undefined js/undefined (fn [error result-code]
                                                                                         (js/console.log "Failure installing Chrome extension: " result-code " - " error)))))}
       [:img {:src "/images/add-to-chrome.png"
              :alt "Chrome"}]]]
     [:div.leader
      [:a#firefox.integration-button
       {:href "https://addons.mozilla.org/en-US/firefox/addon/slacky/"
        :target "_blank"
        :on-click #(event! "firefox-install")}
       [:img {:src "/images/add-to-firefox.png"
              :alt "Firefox"}]]]]

    [:div.col-xs-12.col-md-6 {:style {:border "none" :text-align "center"}}
     [:iframe {:src "https://player.vimeo.com/video/147954001?title=0&byline=0&portrait=0"
               :width "100%"
               :height 250
               :frameBorder 0
               :webkitallowfullscreen true
               :mozallowfullscreen true
               :allowFullScreen true}]]]])
