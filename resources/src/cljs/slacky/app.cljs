(ns slacky.app
  (:require [slacky.routes]
            [slacky.nav :as nav]))

(enable-console-print!)
(println "Hello world! Slacky is here! cljs running")

(println (nav/get-token))
((nav/nav! (nav/get-token)))
