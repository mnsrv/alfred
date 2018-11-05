(defproject alfred "0.0.1"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [clj-http            "3.9.1"]
                 [environ             "1.1.0"]
                 [morse               "0.4.0"]]

  :plugins [[lein-environ "1.1.0"]]

  :main ^:skip-aot alfred.core
  :target-path "target/%s"

  :profiles {
    :uberjar {
      :aot :all
      :uberjar-name "alfred.jar"
    }
  })
