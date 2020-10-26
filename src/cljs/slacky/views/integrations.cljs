(ns slacky.views.integrations
  (:require [slacky.nav :as nav]
            [slacky.analytics :refer [event!]]))

(defn slack-success []
  [:div
   [:p "Start using in Slack right away by typing "
    [:code "/meme"]
    " in any channel"]
   [:p "Stuck? Type "
    [:code "/meme :help"]
    " or try the demos on this page"]])

(defn slack-failure []
  [:div
   [:p "Something went wrong. Contact me at @oliyh on Twitter if you need help!"]])

(defn slack-denied []
  [:div
   [:p "You will have to authorise this service to be able to use it in Slack."]])

(defn component [slack-oauth-url]
  [:div.jumbotron
   [:div.row
    [:div.col-xs-12.col-md-6
     [:div.leader
      [:div
       [:span.h1 "Get it!"]]

      [:a#slack.integration-button.clickable
       {:href slack-oauth-url
        :target "_blank"
        :on-click #(event! "slack-install")}
       " "
       [:img {:alt "Add to Slack"
              :height 80
              :width 278
              :src "https://platform.slack-edge.com/img/add_to_slack@2x.png"}]]]]

    [:div.col-xs-12.col-md-6 {:style {:border "none" :text-align "center"}}
     [:iframe {:src "https://player.vimeo.com/video/138360289?title=0&byline=0&portrait=0"
               :width "100%"
               :height 230
               :frameBorder 0
               :allowFullScreen true}]]]

   [:div.row
    [:div.col-xs-12.col-md-6
     [:div.leader
      [:a#chrome.integration-button.clickable
       {:href "#chrome"
        :on-click #(do (event! "chrome-install")
                       (when (and js/window.chrome.webstore (not js/window.chrome.app.isInstalled))
                         (.install js/window.chrome.webstore js/undefined js/undefined (fn [error result-code]
                                                                                         (js/console.log "Failure installing Chrome extension: " result-code " - " error)))))}
       [:img {:src "/images/add-to-chrome.png"
              :alt "Chrome"}]]]
     [:div.leader
      [:a#firefox.integration-button.clickable
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
               :allowFullScreen true}]]]])
