(ns clojure-lsp.feature.clean-ns-test
  (:require
   [clojure-lsp.feature.clean-ns :as f.clean-ns]
   [clojure-lsp.shared :as shared]
   [clojure-lsp.test-helper :as h]
   [clojure.set :as set]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(h/reset-components-before-test)

(defn- test-clean-ns
  ([db input-code expected-code]
   (test-clean-ns db input-code expected-code true))
  ([db input-code expected-code in-form]
   (test-clean-ns db input-code expected-code in-form "file:///a.clj"))
  ([db input-code expected-code in-form uri]
   (h/reset-components!)
   (swap! (h/db*) shared/deep-merge db)
   (h/load-code-and-locs input-code (h/file-uri uri))
   (let [zloc (when in-form
                (-> (z/of-string input-code) z/down z/right z/right))
         [{:keys [loc range]}] (f.clean-ns/clean-ns-edits zloc (h/file-uri uri) (h/db))]
     (is (some? range))
     (is (= expected-code
            (z/root-string loc))))))

(deftest clean-ns-test
  (testing "prune duplicated imports"
    (test-clean-ns {}
                   (h/code "(ns foo.bar"
                           " (:require"
                           "  [other.foo :as F]"
                           "  [other.foo :as F]))")
                   (h/code "(ns foo.bar"
                           " (:require"
                           "  [other.foo :as F]))"))))

(def data-string
 "(ns foo.bar 
    (:require [other.foo :as F]  [other.foo :as F] 
 [other.kkk :as A] 
 [other.foo :as B] 
              [other.foo :as F]))"
)


(def zloc (z/of-string data-string))
(def req-loc (-> zloc z/down z/right z/right z/down))

(comment
  (defn next-state [{:keys [i sss zloc] :as state}]
    (let [new-zloc (if (= i 6) 
                     (z/next zloc)
                     (z/remove zloc)) 
      _ (clojure.pprint/pprint {:debug "next-state new-zloc" :data new-zloc})
          new-sexpr (z/sexpr new-zloc)]
      (clojure.pprint/pprint {:debug "next-state new-sexpr" :data new-sexpr})
      {:sss sss
       :i (inc i)
       :zloc new-zloc
       :str (set [(str new-sexpr)])
       :zexpr new-sexpr}))

  ;; take-while nil
  (let 
    [
     #_#_ x (clojure.pprint/pprint {:debug "INIT _" :data nil})
     _ (z/print-root zloc)
     res (->> {:i 0 :sss #{} :zloc zloc :zexpr (z/sexpr zloc)}
              (iterate next-state)
              (take 30)
              #_(map #(str (:i %)  " - " (:str %) "\n"))
              #_println
              (last)
              )]
    (z/print-root (:zloc res))))

(let [loc (-> req-loc z/right z/right)]
  {:loc (z/sexpr (z/subedit-> loc))
   :req-loc (z/sexpr req-loc)})


;; ... and a little helper that navigates our location to the end node:
(defn to-end [zloc]
  (->> zloc
       (iterate z/next)
       (drop-while (complement z/end?))
       first))

(z/subedit-> zloc
             (z/find-value z/next 'F)
             (z/up)
             z/remove)

(def node
  (-> zloc
      (z/find-value z/next 'F)
      (z/up)
      (z/node)))

; WHUUUUUUUUUUUUUUT
(defn zloc->str
  [zloc]
  (with-out-str (-> zloc z/print)))

(defn str->set [string]
  (set [string]))
