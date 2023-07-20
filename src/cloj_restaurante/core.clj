(ns cloj-restaurante.core
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.set :as set-op]))

(def h2-db {:dbtype "h2"
            :user "sa"
            :dbname "./resources/myrest.data.mv.db"
            :classname "org.h2.Driver"
            :port "8082"
            })

(j/db-do-commands h2-db
                  [(j/drop-table-ddl :orders)
                   (j/drop-table-ddl :menu)
                   
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

(defn save-order [order]
  (let [json-order (order-to-json order)]
    (j/insert! h2-db :orders {:the_order json-order
                              :date (java.time.LocalDateTime/now)})))

(def food (let [menu-food (j/query h2-db ["select name from menu"])]
            (map #(get % :name) menu-food)))

(defn pretty-string-menu [food-map]
  (let [name (get food-map :name)
        price (str "\t| " (get food-map :price) "\n")]
    (str name price)))

(def food-with-prices (let [menu-food (j/query h2-db ["select name,price from menu"])]
            (map #(pretty-string-menu %) menu-food)))

(defn generate-random-order
  ([] (take (+ (rand-int 4) 1) (repeatedly #(rand-nth food))))
  ([size] (take size (repeatedly #(rand-nth food)))))

(defn food-exist? [& food-input]
  (let [food-input-set (set '(food-input))
        all-food-set (set food)]
    (set-op/subset? food-input-set all-food-set)))

(defn take-order* []
  (let [is-number? (fn [maybe-number]
          (not (nil? (re-matches #"\d+" maybe-number))))

        multiple-diverse-food? (fn [maybe-multiple-diverse-food]
                                 (count maybe-multiple-diverse-food)]
    
    (println "Nombre de la comida\t|Precio")
    (println food-with-prices)
    (println "food ntimes | food [another food]... | END")
  
  (as-> (read-line) user-input
    (while (not= "END" user-input)
      (let [split-food-input (str/split user-input #" ")]        
        (if (and (is-number? (nth split-food-input 2 0))
                 (>= 2(count split-food-input)))
          (println "comida y numero")
          (save-order split-)))))))
    ;; customer

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
