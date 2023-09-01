(ns cloj-restaurante.datafood
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.data.json :refer [write-str]]
            [clojure.set :refer [subset?]]))

(def h2-db {:dbtype "h2"
            :user "sa"
            :dbname "./resources/myrest"
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

(defn order-to-json [order]
  (write-str (frequencies order)))

(defn save-order
  ([multiple-food]
   (println multiple-food)
   (let [json-order (order-to-json multiple-food)]
     (println (str "2: " json-order))
     (j/insert! h2-db "orders" {:the_order json-order
                                :date (java.time.LocalDateTime/now)})))
  ([food number]
   (let [json-order (write-str (hash-map food number))]
     (j/insert! h2-db "orders" {:the_order json-order
                                :date (java.time.LocalDateTime/now)}))))

(def food (atom (let [menu-food (j/query h2-db ["select name from menu"])]
                  (map #(get % :name) menu-food))))


(defn is-order-correct? [order]
  (println (class order) order)
  (subset? (set order) (set @food)))

(defn- pretty-string-menu [food-map]
  (let [name (get food-map :name)
        price (str "\t| " (get food-map :price) "\n")]
    (str name price)))

(def food-with-prices (atom (j/query h2-db ["select name,price from menu"])))
