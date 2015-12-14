(ns user
  (:require [clojure.tools.namespace.repl :as repl]
     ;;       [cemerick.piggieback :refer [cljs-repl]]
            ))

(defn dev []
  (require 'dev)
  (in-ns 'dev)
  #_ (dev/start))

(def refresh repl/refresh)

#_(defn cljs-repl []
  (cljs-repl (cljs.repl.rhino/repl-env)))
