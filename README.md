# trees

A catchall utility library for tree related data structures

## Usage


Under construction.

```clojure
(use 'trees.labeled-trie)

(def m (reduce (fn [tree ks] (tree-update-in tree ks last-val-sum safe-inc)) (create-root) [[:a 1] [:a 1] [:a 4] [:a 9]]))
 
;; #trees.labeled_trie.Node{:count 4, :agg 15, :children {:a #trees.labeled_trie.Node{:count 4, :agg 15, :children {1 1, 4 0, 9 0}}}}

```

## Labeled Trie 

This is a tree data structured built out of maps  and sorted maps (TreeMap).  
At each odd level (root == 0) a sorted-map is created and each even level creates a normal hash map.  

Unlike normal Trie's the key is not a String but rather a sequence e.g [:a 1 :b 2 :c 3].  
The whole odd sorted level path setup is meant to support labeled paths i.e [label1 val1 label2 val2 ...]  
So labels are not sorted by values are.  

This allows data to be stored e.g in a time series [:ts millis :grouping1 1 :grouping2 2]  
If you get label :ts the millis values are in sorted order, the same if you do (get tree [:ts :grouping1]) etc..   

### Hierarchical Aggregations

The Labeled Trie should actually be called a Labeled Hierarchical Trie, becuase it allows to stored aggregations at each node level  
in a path. This feature is usefully for results like sum and count that are commutative and makes for fast querying where results  
are already pre-computed and doesn't need a full path scann.  

Each Node at each level maintains the key works :count and :agg.  
The keyword :count contains the number of updates made to all subnodes.  
The keyword :agg contains whatever was returned by the agg functions, see "Tree updates/inserts".  

### Tree updates/inserts

Updates and inserts are the same and uses ```tree-update-in```  
The function takes the arguments [tree ks agg-f update-f]

```
tree     => is the tree returned from either create-root or another tree-* function
ks       => is a sequence (array, collection or vector) that contains the path e.g :a 1 :b 2 :c 3
agg-f    => Each node at each level has a :agg key which is used to support hierarchical aggregation values, 
            the agg-f each called at each node level in the ks update path with the arguments, (agg-f agg lvl all-ks),
            its arguments are agg    => the previous value returned by agg or nil,
                              lvl    => the level 0 == root at which the node is at,
                              all-ks => the ks as passed into the tree-update-in function
update-f => Is the function called at the end of the ks path (note any intermediate nodes are automatically created.
            The function is called (apply update-f (get (:children m) k) update-f-args),
            its arguments are (update-f state args)
            state => is the last value returned at the update path ks i.e if no value existed this will be nil otherwise if this function returned a vector [1 2] 
                     state will be [1 2] or as with the safe-inc function if it returned 0, 0 will be the state value.
```


### Benchmark

#### Insert 100 000 random records with 3 labels i.e a depth of 6.

```
WARNING: Final GC required 3.2728941684015633 % of runtime
Goal:  Insert into labeled trie
-----
Case:  :labeled-insert
Evaluation count : 120 in 60 samples of 2 calls.
             Execution time mean : 596.872507 ms
    Execution time std-deviation : 3.593147 ms
   Execution time lower quantile : 590.464499 ms ( 2.5%)
   Execution time upper quantile : 602.800874 ms (97.5%)
                   Overhead used : 1.861573 ns
```

## License

Copyright © 2014 Gerrit Jansen van Vuuren

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
