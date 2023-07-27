(ns cloj-restaurante.core
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [clojure.main :as main]
            [clojrue.set]))

(def h2-db {:dbtype "h2"
            :user "sa"
            :dbname "./resources/myrest"
            :classname "org.h2.Driver"
            :port "8082"
            })

(j/db-do-commands h2-db
                  [;;(j/drop-table-ddl :ordeprs)
                   ;;(j/drop-table-ddl :menu)
                   (j/create-table-ddl :menu
                                       [[:id :bigint :auto_increment :primary :key]
                                        [:name "varchar(32)" "not null"]
                                        [:price :int "not null"]] true)
                   (j/create-table-ddl :orders
                                       [[:id :bigint :auto_increment :primary :key]
                                        [:the_order :json "not null"]
                                        [:date :date "not null"]
                                        [:deadline :date]])])

(j/insert-multi! h2-db :menu
                 [{:name "arroz" :price 50}
                  {:name "tortilla" :price 100}
                  {:name "refresco" :price 60}
                  {:name "lomo" :price 90}
                  {:name "bienmesabe" :price 80}]) 

(def order-to-json (comp json/json-str frequencies))





(defn save-order
  ([multiple-food]
   (let [json-order (order-to-json multiple-food)]
     (j/insert! h2-db :orders {:the_order json-order
                               :date (java.time.LocalDateTime/now)})))
  ([food number]
   (let [json-order (json/json-str (hash-map food number))]
     (j/insert! h2-db :orders {:the_order json-order
                               :date (java.time.LocalDateTime/now)}))))

(def food (let [menu-food (j/query h2-db ["select name from menu"])]
            (map #(get % :name) menu-food)))

(defn is-order-correct? [order]
  (println (class order) order)
  (clojure.set/subset? (set order) (set food)))

(def repl-options
  [:prompt #(printf "Introduzca la orden :> ")
   :read   (fn [request-prompt request-exit]
             (or ({:line-start request-prompt :stream-end request-exit}
                  (main/skip-whitespace *in*))
                 (s/split (read-line) #" ")))
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

(defn pretty-string-menu [food-map]
  (let [name (get food-map :name)
        price (str "\t| " (get food-map :price) "\n")]
    (str name price)))

(def food-with-prices (let [menu-food (j/query h2-db ["select name,price from menu"])]
                        (map #(pretty-string-menu %) menu-food)))

(defn generate-random-order
  ([] (take (+ (rand-int 4) 1) (repeatedly #(rand-nth food))))
  ([size] (take size (repeatedly #(rand-nth food)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
