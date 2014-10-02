(defproject trees "0.1.0-SNAPSHOT"
  :description "A catch all utility library for creating tree data structures "
  :url "https://github.com/gerritjvv/trees"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[perforate "0.3.3"] [lein-midje "3.1.1"]]

  :perforate {:benchmark-paths ["bench"]}

  :javac-options ["-target" "1.5" "-source" "1.5" "-Xlint:-options"]

  :global-vars {*warn-on-reflection* true
                *assert* false}

  :jvm-opts ["-Xmx1g"
             "-server"
             ;"-XX:+UnlockDiagnosticVMOptions"
             ;"-XX:+PrintInlining"
             ;"-XX:MaxInlineSize=600"

             ;"-XX:+PrintCompilation"
             ;"-XX:CompileThreshold=100"
             ]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/nippy "2.6.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [aleph "0.3.3"]
                 [com.stuartsierra/component "0.2.2"]
                 [midje "1.6.3" :scope "test"]
                 [criterium "0.4.3" :scope "provided"]])
