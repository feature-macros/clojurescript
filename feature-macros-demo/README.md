# Feature Macros &mdash; an Alternative to Feature Expressions

*[Discuss on Hacker News](https://news.ycombinator.com/item?id=8923385)*

[Feature Expressions][fx] is a proposal to ease the creation and
consumption of portable Clojure code.  Among other things, it proposes
extending the syntax of Clojure with two new reader macros, `#+` and
`#-` or "feature include" and "feature exclude".

While the proposal dates to 2012, these reader macros are even older.
They're in [Common Lisp][hyperspec] and we've seen them more recently
in [cljx].

Even though Common Lisp is immortal and cljx is popular, we think the
idea of adding this syntax to Clojure is terrible.  `#+` and `#-` are
constructs we can type easily, but they evade all but the most
rudimentary means of combination and abstraction.  As syntax they
never exist as values, and so can't be passed as arguments,
participate in scopes, or be returned from functions or macros. **We
can type them, but we can't automate the typing of them.**  We've
already learned from our experience with non-Lisp languages how depressing
this situation can be.

While the idea is bad, the growing pains it addresses are real.  Clojure
continues to increase in popularity across all of its platforms, and
few things would be better for Clojure's health than a unified library
ecosystem.

We propose solving the problem of portability not with new syntax, but
with an existing abstraction: the **macro**.  Instead of asking "What
syntax can we add to solve the problem?" we ask "Why can't we solve
what is clearly a problem of conditional evaluation with macros?"

We found a macro-based solution not only possible, but superior. We call
the set of conventions and enhancements we propose collectively “Feature
Macros”.  Feature Macros require only:

* Adherence to that subset of Clojure syntax in our source code which is
  universally `read`-able.
* Some miniscule, backward-compatible enhancements to Clojure and ClojureScript.
* An abiding belief that **Lisp Can Do It**.

In return, they provide a number of desirable affordances:

* Open, not closed: new platforms can be added by the platform's compiler by
  simply binding a dynamic var when cross compiling&mdash;no need to modify
  Clojure's core libraries, compiler, or reader.
* Extensible by the user: we're programming with values now; the full power
  of the language can be used to extend it.
* Syntax and semantics are properly decoupled, smoothing the way for more
  powerful metaprogramming and tooling.

## Examples

Here are a few examples alongside cljx to get a feel for what Feature Macros
look like:

*With Feature Expressions:*

```clojure
(ns cemerick.pprng
 #+cljs (:require math.seedrandom
                  [cljs.core :as lang])
 #+clj  (:require [clojure.core :as lang])
 #+clj  (:import java.util.Random)
 (:refer-clojure :exclude (double float int long boolean)))
```

*With Feature Macros:*

```clojure
(ns+ cemerick.pprng
  (:cljs (:require math.seedrandom
                   [cljs.core :as lang]))
  (:clj  (:require [clojure.core :as lang]))
  (:clj  (:import java.util.Random))
  (:refer-clojure :exclude (double float int long boolean)))
```

*With Feature Expressions:*

```clojure
(.getTime
  #+clj (java.util.Date.)
  #+cljs (js/Date.))

#+clj
(defn url-encode
  [string]
  (some-> string str (URLEncoder/encode "UTF-8") (.replace "+" "%20")))
#+cljs
(defn url-encode
  [string]
  (some-> string str (js/encodeURIComponent) (.replace "+" "%20")))

#+clj (set! *warn-on-reflection* true)
```

*With Feature Macros:*

```clojure
(.getTime
  (case-host
    :clj  (java.util.Date.)
    :cljs (js/Date.)))

(case-host
  :clj
  (defn url-encode
    [string]
    (some-> string str (URLEncoder/encode "UTF-8") (.replace "+" "%20")))
  :cljs
  (defn url-encode
    [string]
    (some-> string str (js/encodeURIComponent) (.replace "+" "%20"))))

(case-host :cljs nil :clj (set! *warn-on-reflection* true))
```

And some things that Feature Expressions can't do:

*Cross-platform macros:*

```clojure
(case-host
  :cljs nil
  :clj  (defmacro my-macro [name & body]
          (case-target
            :clj  `(.println (System/-out) (str (do ~@body)))
            :cljs `(.log js/console (str (do ~@body))))))
```

*Userland extensions:*

```clojure
(defmacro +clj
  "Form is evaluated only in the JVM."
  [form]
  (when (= :clj *host*) form))
```

Have a look at the source files in this demo directory [for][core] [more][util]
[examples][url].

## Requirements

Portable code, which we define as “code written with some expectation of
functionality across two or more platforms”, must be readable on every
platform, because a macro may consume and emit code for platforms other
than the one it’s running on. Portable code should not contain:

* **regex literals** &mdash; use `re-pattern` in cross-platform source instead.
* **tagged literals** &mdash; use `(js {"foo" "bar"})` instead of `#js {“foo” “bar”}`.
* **read-eval** or `#=()` &mdash; use a macro instead.

> **Note:** The tagged literal restriction need not apply to runtime
> serialization of data, eg. reading or writing EDN via `tools.reader`,
> because platform-specific tag readers are available at runtime, via
> Feature Macros, even. While tagged literals may be useful for transmitting
> rich data, this richness has very little value for the representation of
> programs. Complecting program representation with runtime serialization
> concerns is, we feel, a mistake.

## Implementation

```clojure
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
```

* New dynamic vars in core, `*host*` and `*target*`, the officially
  recognized values of which include `:clj` and `:cljs`. Compilers bind
  these to the proper platform when cross compiling.
* Dispatch on `*host*` in platform-specific code, dispatch on `*target*`
  in macros that emit platform-specific code.
* New macros in core, `case-host` and `case-target`, for conditionally
  emitting code depending on the values of `*host*` and `*target*`,
  respectively.
* New `ns+` macro in core, for conditionally emitting namespace declaration
  clauses depending on the value of `*host*`.

## Q &amp; A

* What about DSLs other than `ns`, like `core.match`, where Clojure
  semantics are void?
  * Macros can't work where macroexpansion doesn't occur, but all the
    FX examples we've found can be trivially reorganized to use just
    Feature Macros.
  * Feature Macros don't preclude cljx if one still prefers that
    syntax.
  * At a deeper level, areas in source code without Clojure semantics
    are really windows into *languages other than Clojure*.  If
    anything about these languages can be platform-dependent, then
    they too should participate in the system of `*host*` and
    `*target*` so that users may extend them portably.
* What about the "feature set" idea to support
  [implementation-dependent features][features] like `:arch/osx` in
  addition to just platforms like `:clj` and `:cljs`?
  * Unlike Common Lisp, Clojure is not a platform&mdash;it is
    symbiotic with many platforms.  Clojure need only define a
    platform-independent way of determining platform.  Then, users can
    write platform-specific code to determine platform-specific
    functionality.
* How about `*foo*` and `*bar*` instead of `*host*` and `*platform*`?
  Also, which file extensions should be used?
  * We just wanted to show the general idea and that it works.  We
    don't recommend particular names and left supporting details
    unspecified.
* What about read-time elision of source code?
  * It's not possible to program with values in the absence of them.
    In Clojure, even the concept of nothing has a workable value:
    `nil`.  To elide source is to make an imperative statement without
    even returning `nil`, and opts you out of homoiconicity.  You are
    doomed to string preprocessing forever.  Source elision was only
    ever absolutely necessary in Lisp implementations that attempted
    to support multiple syntaxes, which is fortunately not something
    Clojure will ever need to support.

# Demo

This directory contains a small application demonstrating the use of
Feature Macros. The Clojure implementation is provided by the [boot-shim.clj]
file, which is loaded automatically by [boot] to monkeypatch Clojure's core.
ClojureScript support is provided by [this commit].

### Install, Build, See

Open a terminal in this directory and do:

```bash
make deps # install dependencies, CLJS compiler
```
```bash
make demo # compile CLJS demo and run CLJ demo
```
```bash
open target/index.html # open CLJS demo in browser
```

[fx]: http://dev.clojure.org/display/design/Feature+Expressions
[hyperspec]: http://www.lispworks.com/documentation/HyperSpec/Body/24_aba.htm
[cljx]: https://github.com/lynaghk/cljx
[boot-shim.clj]: https://github.com/feature-macros/clojurescript/blob/feature-macros/feature-macros-demo/boot-shim.clj
[this commit]: https://github.com/feature-macros/clojurescript/commit/e2f7c8353b87ef4500d971698f567d509280609c
[boot]: https://github.com/boot-clj/boot
[url]: https://github.com/feature-macros/clojurescript/blob/feature-macros/feature-macros-demo/src/cemerick/url.clj
[util]: https://github.com/feature-macros/clojurescript/blob/feature-macros/feature-macros-demo/src/demo/util.clj
[core]: https://github.com/feature-macros/clojurescript/blob/feature-macros/feature-macros-demo/src/demo/core.clj
[features]: http://www.lispworks.com/documentation/HyperSpec/Body/v_featur.htm
