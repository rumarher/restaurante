(ns cloj-restaurante.core
  (:gen-class)
  (:require
   [kafkaprod :refer send-order-topic]
   [datafood :refer [food]]
   [userside :as us]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
