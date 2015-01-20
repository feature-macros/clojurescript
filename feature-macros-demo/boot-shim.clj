(in-ns 'clojure.core)

(def ^:dynamic *host*   :clj)
(def ^:dynamic *target* :clj)

(defmacro ns+ [& clauses]
  (let [require-ops    #{:refer-clojure :require :use :import :load
                         :gen-class :require-macros :use-macros}
        this-platform? #(cond (or (not (seq? %)) (require-ops (first %))) %
                              (= *host* (first %)) (second %))]
    `(~'ns ~@(keep this-platform? clauses))))

(defmacro case-host [& pairs]
  (get (apply hash-map pairs) *host*
       `(throw (ex-info "Unsupported host platform" {:host *host*}))))

(defmacro case-target [& pairs]
  (get (apply hash-map pairs) *target*
       `(throw (ex-info "Unsupported target platform" {:target *target*}))))
