(ns slacky.views.footer
  (:require [slacky.nav :refer [nav!]]))

(defn privacy []
  [:div
   [:p "We store a randomly generated token to identify your account, allowing
you to take advantage of various features."]
   [:p "Users of the Slack integration will have any interaction transmitted via Slack's servers."]])

(defn component []
  [:div#footer
   [:p [:small "Contact "
        [:a {:href "https://twitter.com/oliyh"
             :target "_blank"} "@oliyh"]
        " or "
        [:a {:href "mailto://slackydev@gmail.com"}
         "slackydev@gmail.com"]]
    " / "
    [:small [:a {:href "https://github.com/oliyh/slacky"
                 :target "_blank"}
             "View on Github"]]
    " / "
    [:small [:a {:href "/privacy"
                 :on-click (nav! "/privacy")}
             "Privacy policy"]]]

   [:p [:small "We are not created by, affiliated with, or supported by either Slack Technologies, Inc., Google or Mozilla"]]])
