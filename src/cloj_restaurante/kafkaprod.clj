(ns cloj-restaurante.kafkaprod
  (:gen-class)
  (:require [clojure.java.data :as j])
  (:import (org.apache.kafka.clients.producer KafkaProducer ProducerConfig ProducerRecord)
           (org.apache.kafka.common.serialization StringSerializer)))

(println "hola mundo")

(def kafka-properties
  (j/to-java java.util.Properties
             {"bootstrap.servers" "localhost:9092"
              "key.serializer" "org.apache.kafka.common.serialization.StringSerializer"
              "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"}))

(def producer (KafkaProducer. kafka-properties))

(defn send-order-topic [order-in-json]
  (.send producer (new ProducerRecord (hash order-in-json) order-in-json)))
