(ns trees.labeled-trie)


(defrecord Node [count children])

(comment
  ;usage
  (def m (tree-update-in 0 (create-root) [:ts (System/currentTimeMillis) :a 1 :b 2] safe-inc ))

  )

(defn create-root [] (->Node 0 {}))

(defn- tree-assoc [lvl m k v]
  (let [m2 (if m m (if (odd? lvl) (->Node 0 (sorted-map)) (->Node 0 {})))]
   (assoc m2 :count (inc (:count m2)) :children (assoc (:children m2) k v))))


(defn tree-update-in
  ([m ks f & args]
   (apply tree-update-in 0 m ks f args))
  ([lvl m [k & ks] f & args]
   (if ks
     (tree-assoc lvl m k (apply tree-update-in (inc lvl) (get m k) ks f args))
     (tree-assoc lvl m k (apply f (get m k) args)))))



(defn safe-inc
  ([]  0)
  ([v]
   (if v (inc v) 0)))

(defn safe-sum
  ([] 0)
  ([& vs] (apply + vs)))

(defn tree-get-in [{:keys [children]} [k & ks]]
  (when-let [node (get children k)]
    (if (and (record? node) (:children node))
      (if ks
        (tree-get-in node ks)
        node)
      node)))
