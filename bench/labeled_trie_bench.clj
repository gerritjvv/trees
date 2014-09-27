 (ns labeled-trie-bench
   (:require [trees.labeled-trie :as t])
   (:use perforate.core))


 (defn gen-data []
   (take 10000
         (repeatedly (fn []
                       [:a (rand-int 10) :b (rand-int 10) :c (rand-int 100)]))))

 (defgoal insert "Insert into labeled trie"
          :setup (fn [] [(doall (gen-data))]))


; (def m (tree-update-in 0 (create-root) [:ts (System/currentTimeMillis) :a 1 :b 2] safe-inc ))

(defcase insert :labeled-insert
          [data]
          (reduce (fn [tree ks]
                    (t/tree-update-in 0 tree ks t/safe-inc)) (t/create-root) data))