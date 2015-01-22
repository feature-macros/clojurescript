(ns+ demo.util
  (:clj  (:refer-clojure :exclude [format]))
  (:cljs (:require [goog.string :as gstring]
                   [goog.string.format])))

(case-platform
  :cljs nil
  :clj (defmacro log-err [message]
         (condp = *platform*
           :clj  `(.println (System/-err) ~message)
           :cljs `(.warn js/console ~message))))

(defn format [fmt & args]
  (case-platform
    :clj  (apply clojure.core/format fmt args)
    :cljs (apply gstring/format fmt args)))

