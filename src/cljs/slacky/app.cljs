(ns slacky.app
  (:require [slacky.routes]
            [slacky.nav :as nav]))

(enable-console-print!)
(println "Hello world! Slacky is here! cljs running")
((nav/nav! (nav/get-token)))
