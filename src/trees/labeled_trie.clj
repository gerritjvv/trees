(ns trees.labeled-trie)


(defrecord Node [^Long count agg children])

(comment
  ;usage
  (def m (tree-update-in (create-root) [:ts (System/currentTimeMillis) :a 1 :b 2] safe-sum safe-inc ))

  ;;aggregation function
  ;;The
  )

(defn create-root [] (->Node 0 0 {}))


(defn- tree-assoc
  "all-ks All the original keys and values passed to the update-in
   lvl the current level in the tree
   m the current tree
   k the current key
   v the current node
   agg-f the aggregation function that is applied to each node at each level in the path being updated
   "
  [all-ks lvl m k v agg-f]
  (let [{:keys [count agg children] :as m2} (if m m (if (odd? lvl) (->Node 0 0 (sorted-map)) (->Node 0 0 {})))]
   (assoc m2
     :count (inc count)
     :agg (agg-f agg lvl all-ks)
     :children (assoc children k v))))



(defn- _tree-update-in
  "all-ks All the original keys and values passed to the update-in
   lvl the current level in the tree
   m the current tree
   ks the current keys
   agg-f the aggregation function
   f the function applied to the value stored at the end of the path as marked by ks
   "
  ([all-ks ^Long lvl m [k & ks] agg-f f]
   (if ks
     (tree-assoc all-ks lvl m k (_tree-update-in all-ks (inc lvl) (get (:children m) k) ks agg-f f) agg-f)
     (tree-assoc all-ks lvl m k (f (get (:children m) k)) agg-f))))

(defn tree-update-in [m ks agg-f f]
  (_tree-update-in ks 0 m ks agg-f f))

(defn tree-get-in [{:keys [children]} [k & ks]]
  (when-let [node (get children k)]
    (if (and (record? node) (:children node))
      (if ks
        (tree-get-in node ks)
        node)
      node)))

(defn tree-count-in [tree ks]
  (-> tree (tree-get-in ks) :count))

(defn tree-agg-in [tree ks]
  (-> tree (tree-get-in ks) :agg))

(defn safe-number
  "If no argument of the argument is nil or is not a number 0 is returned"
  ([] 0)
  ([v] (if v v 0)))

(defn safe-inc
  ([]  1)
  ([v]
   (if v (inc v) 1)))

(defn safe-sum
  ([] 0)
  ([^long v1 ^long v2]
   (+ v1 v2)))

(defn last-val-sum
  ([] 0)
  ([acc lvl ks]
   (+ (safe-number acc) (-> ks last safe-number))))




(defn gen-data []
  (doall
    (take 1000000
          (repeatedly (fn []
                        [:a (rand-int 10) :b (rand-int 10) :c (rand-int 100)])))))



(defn test-run
  [data]
  (reduce (fn [tree ks]
            (tree-update-in tree ks last-val-sum safe-inc)) (create-root) data))
