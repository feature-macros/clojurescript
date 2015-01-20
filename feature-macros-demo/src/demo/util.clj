(ns+ demo.util
  (:clj  (:refer-clojure :exclude [format]))
  (:cljs (:require [goog.string :as gstring]
                   [goog.string.format])))

(case-host
  :cljs nil
  :clj (defmacro log-err [message]
         (case-target
           :clj  `(.println (System/-err) ~message)
           :cljs `(.warn js/console ~message))))

(defn format [fmt & args]
  (case-host
    :clj  (apply clojure.core/format fmt args)
    :cljs (apply gstring/format fmt args)))

