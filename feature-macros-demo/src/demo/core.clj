(ns+ demo.core
  (:clj  (:require [cemerick.url :as url]
                   [demo.util    :as util]))
  (:cljs (:require [cemerick.url :as url]
                   [demo.util    :as util :include-macros true])))

(def url
  {:protocol "https"
   :host     "github.com"
   :path     "/feature-macros/clojurescript"})

(defn -main [& _]
  (util/log-err (util/format "hello world: %s" (url/map->URL url))))
