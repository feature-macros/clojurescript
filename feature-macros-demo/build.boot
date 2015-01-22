(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"html"}
 :dependencies   '[[pathetic "0.5.0"]
                   [org.clojure/clojurescript "0.0-2694"]
                   [adzerk/boot-cljs "0.0-2629-8"]])

(require '[demo.core :as core]
         '[clojure.java.io :as io]
         '[adzerk.boot-cljs :refer [cljs]]
         '[boot.from.io.aviso.ansi :refer :all])

(defn say [msg]
  (with-pre-wrap fs (info (green (str msg "\n"))) fs))

(deftask run
  "Run CLJ demo code."
  []
  (with-pre-wrap fs (core/-main) fs))

(deftask demo
  "Compile CLJS and run CLJ demos."
  []
  (comp
    (say "** COMPILING CLJS DEMO **")
    (cljs)
    (say (format "file://%s/target/index.html" (System/getProperty "user.dir")))
    (say "** RUNNING CLJ DEMO **")
    (run)
    (say "** TRY FEATURE MACROS IN THE REPL **")
    (repl)))
