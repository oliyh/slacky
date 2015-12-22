(ns slacky.views.demo)

(defn- example [img-src command]
  [:div.col-xs-12.col-md-4
   [:div.example {:data-command command}
    [:img.img-thumbnail {:src img-src}]
    [:code command]]])

(defn component []
  [:div.jumbotron
   [:a {:name "demo"}]
   [:div.leader
    [:h1 "How can haz meme?"]]

   [:div.row
    [example "/images/pai-mei-approves.png" "pai mei | pai mei | approves"]
    [example "/images/slacky-wins.png" "https://goo.gl/h9eUDM | slacky | wins"]
    [example "/images/all-the-memes.png" "create all the memes!"]]

   [:div.row
    [:div.col-xs-12
     [:form#demo.form-horizontal
      [:div.col-xs-12.col-md-11
       [:div.form-group.form-group-lg
        [:div.input-group
         [:div.input-group-addon "/meme"]
         [:input#demo-text.form-control {:type "text"
                                         :placeholder "search term or url | upper text | lower text"}]]]]
      [:div.col-xs-12.col-md-1
       [:div.form-group.form-group-lg
        [:button.btn.btn-success.btn-lg {:type "submit"}
         "Try!"]]]]]]])
