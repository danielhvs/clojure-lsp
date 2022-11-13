(ns clojure-lsp.feature.clean-ns-test
  (:require
   [clojure-lsp.feature.clean-ns :as f.clean-ns]
   [clojure-lsp.shared :as shared]
   [clojure-lsp.test-helper :as h]
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
                           "  [other.foo F]"
                           "  [other.foo F]))")
                   (h/code "(ns foo.bar"
                           " (:require"
                           "  [other.foo F]))"))))
