(in-ns 'clojure.core)

(def ^:dynamic *platform* :clj)

(defmacro ns+ [& clauses]
  (let [require-ops    #{:refer-clojure :require :use :import :load
                         :gen-class :require-macros :use-macros}
        this-platform? #(cond (or (not (seq? %)) (require-ops (first %))) %
                              (= *platform* (first %)) (second %))]
    `(~'ns ~@(keep this-platform? clauses))))

(defmacro case-platform [& pairs]
  (get (apply hash-map pairs) *platform*
       `(throw (ex-info "Unsupported platform" {:platform *platform*}))))

(alter-var-root #'load #(fn [& xs] (binding [*platform* :clj] (apply % xs))))
