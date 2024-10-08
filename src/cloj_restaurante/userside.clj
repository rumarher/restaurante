(ns cloj-restaurante.userside
  (:gen-class)
  (:require [clojure.string  :as str]
            [cloj-restaurante.datafood :refer [food
                                               is-order-correct?
                                               get-order
                                               order-to-cook
                                               order-to-deliver
                                               order-to-refuse
                                               order-to-pending
                                               order-to-be-done
                                               save-order
                                               get-all-orders
                                               get-order-status]]
            [clojure.main :as main]
            [clojure.core.async :refer [chan buffer]]))

(def incoming-orders (chan))

(def cooking-orders (chan))

(def patt-orders #"^\s*(order\s*:|check\s*:|check\s*all)")

(defn start-cooking-order
  [the-order]
  (order-to-cook the-order))

(defn start-deliver
  [the-order]
  (order-to-deliver the-order))

(defn to-refuse
  [the-order]
  (order-to-refuse the-order))

(defn put-in-pending
  [the-order]
  (order-to-pending the-order))

(defn finalize-order
  [the-order]
  (order-to-be-done the-order))

(defn generate-random-order
  ([] (take (+ (rand-int 4) 1) (repeatedly #(rand-nth @food))))
  ([size] (take size (repeatedly #(rand-nth @food)))))

(defn set-new-order
  [pred]
  (let [order-seq (re-seq #"\b(\w+)\s*(\d*)?\b" pred)]
    (save-order
     (apply merge (map (fn [x]
                         (if (clojure.string/blank? (nth x 2))
                           {(second x) 1}
                           {(second x) (nth x 2)}))
                       order-seq)))))

(defn get-instruction
  [data]
  (-> (second (re-find patt-orders data))
      (clojure.string/replace #":" "")
      (clojure.string/replace #"\s+" " ")))

(defn get-predicate
  [data]
  (str/replace data patt-orders  ""))

(defn check-order
  [data]
  (println (get-order data)))

(defn check-order-status
  [data]
  (let [the-order (get-order-status data)]
    (println the-order)))

(defn check-all-orders
  []
  (let [all-orders (get-all-orders)]
    (println all-orders)))

(defn parse-predicate
  [data]
  (re-seq #"\w+\s*\d*" data))

(def repl-options
  [:prompt #(printf "Introduzca la orden :> ")
   :read   (fn [request-prompt request-exit]
             (or ({:line-start request-prompt :stream-end request-exit}
                  (main/skip-whitespace *in*))
                 (read-line)))
   :eval   (fn [the-order]
             (println "la orden -->"  (str the-order))
             (let [the-instruction (get-instruction the-order)
                   pred (get-predicate the-order)]
               (println the-instruction)
               (case the-instruction
                 "order" (set-new-order pred)
                 "check" (check-order pred)
                 "check all" (check-all-orders)
                 (throw (Exception. (str the-instruction ": no es una orden correcta"))))))])
