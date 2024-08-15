(defproject cloj-restaurante "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.clojure/java.data "1.0.95"]
                 [org.clojure/core.async "1.6.673"]
                 [org.clojure/data.json "2.4.0"]
                 [org.apache.kafka/kafka-clients "3.5.1"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors/ring-cors "0.1.13"]
                 [compojure "1.7.0"]
                 [ring/ring-defaults "0.3.4"]
                 [com.h2database/h2 "2.2.220"]
                 [com.fzakaria/slf4j-timbre "0.4.0"]
                 [ring/ring-jetty-adapter "1.10.0"]
                 ;; https://mvnrepository.com/artifact/clj-ulid/clj-ulid
                 [clj-ulid/clj-ulid "1.0.0"]]
  :plugins [[lein-ring "0.12.6"]]
  :ring {:handler cloj-restaurante.core/handler-app
         :init cloj-restaurante.core/save-order-async
         :destroy cloj-restaurante.core/to-exit}
  :main ^:skip-aot cloj-restaurante.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring/ring-mock "0.4.0"]]}})
