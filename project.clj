(defproject steam-deck-badges "0.1.0-SNAPSHOT"
  :global-vars {*warn-on-reflection* true
                *unchecked-math* :warn-on-boxed}
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [org.clojure/data.json "2.4.0"]
                 [http-kit "2.6.0-alpha1"]
                 [metosin/reitit "0.5.18"]
                 [hickory "0.7.1"]
                 [org.clojure/data.json "2.4.0"]
                 ;; java dependencies
                 [com.github.ben-manes.caffeine/caffeine "3.1.0"]]
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :main ^:skip-aot steam-deck-badges.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
