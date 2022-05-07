(defproject steam-deck-badges "0.1.0-SNAPSHOT"
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.11.0"]
                 [org.clojure/data.json "2.4.0"]
                 [http-kit "2.6.0-alpha1"]
                 [hickory "0.7.1"]]
  :main ^:skip-aot steam-deck-badges.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
