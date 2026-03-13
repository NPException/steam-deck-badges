(defproject steam-deck-badges "0.1.0-SNAPSHOT"
  :global-vars {*warn-on-reflection* true
                *unchecked-math* :warn-on-boxed}
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [org.clojure/data.json "2.5.2"]
                 [http-kit "2.8.1"]
                 [metosin/reitit "0.10.0"]
                 [org.clj-commons/hickory "0.7.7"]
                 ;; java dependencies
                 [com.github.ben-manes.caffeine/caffeine "3.2.3"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :main ^:skip-aot steam-deck-badges.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
