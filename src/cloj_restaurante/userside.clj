(ns cloj-restaurante.userside
  (:gen-class)
  (:require [clojure.string :refer [split]]
            [cloj-restaurante.datafood :refer [food is-order-correct?]]
            [clojure.main :as main]
            [clojure.core.async :refer [chan buffer]]))

(def incoming-orders (chan))

(defn generate-random-order
  ([] (take (+ (rand-int 4) 1) (repeatedly #(rand-nth @food))))
  ([size] (take size (repeatedly #(rand-nth @food)))))

(def repl-options
  [:prompt #(printf "Introduzca la orden :> ")
   :read   (fn [request-prompt request-exit]
             (or ({:line-start request-prompt :stream-end request-exit} (main/skip-whitespace *in*))
                 (split (read-line) #" ")))
   :eval   (fn [[& the-order]]
             (let [parsed-order
                   (if (or
                        (= 1 (count the-order))
                        (and (= 2 (count the-order))
                             (re-matches #"\d+" (nth the-order 1))))
                     (and
                      (is-order-correct? (list (nth the-order 0)))
                      (repeat (Integer. (nth the-order 1 0)) (nth the-order 0)))
                     (and
                      (is-order-correct? the-order)
                      the-order))]
               (when-not parsed-order
                 (throw (Exception. (str "la order: " the-order ", es incorrecta."))))
               (println "Buena orden:" the-order " -> " parsed-order)))])
