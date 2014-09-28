 (ns labeled-trie-bench
   (:require [trees.labeled-trie :as t])
   (:use perforate.core))


 (defn gen-data []
   (take 100000
         (repeatedly (fn []
                       [:a (rand-int 10) :b (rand-int 10) :c (rand-int 100)]))))

 (defgoal insert "Insert into labeled trie"
          :setup (fn [] [(doall (gen-data))]))


(defcase insert :labeled-insert
          [data]
          (reduce (fn [tree ks]
                    (t/tree-update-in tree ks t/last-val-sum t/safe-inc)) (t/create-root) data))