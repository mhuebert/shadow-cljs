(defproject thheller/shadow-devtools "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[com.cognitect/transit-cljs "0.8.225"]
                 [http-kit "2.1.19"]
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.122"]
                 [thheller/shadow-client "1.0.0"]
                 [thheller/shadow-build "1.0.123"]]

  :source-paths ["src/clj"
                 "src/cljs"]
  :java-source-paths ["src/java"]

  :profiles {:dev {:source-paths ["src/dev"]
                   ;; :repl-options {:nrepl-middleware [build/browser-dev-nrepl]}
                   }}


  ;; make cursive happy, see https://github.com/cursiveclojure/cursive/issues/665
  ;; shadow-build has nothing to do with lein-cljsbuild!
  :cljsbuild {:builds {:main {:source-paths ["src/cljs" "test-data"]}}}
  )
