(ns clojure.set)

(defn- bubble-max-key [key-fn coll]
  (let [maximum (apply max-key key-fn coll)]
    (cons maximum (remove #(identical? maximum %1) coll))))

(defn union
  ([] #{})
  ([left] left)
  ([left right]
   (if (< (count left) (count right))
     (reduce conj right left)
     (reduce conj left right)))
  ([left right & sets]
   (let [sets (bubble-max-key count (conj sets right left))]
     (reduce into (first sets) (rest sets)))))

(defn intersection
  ([left] left)
  ([left right]
   (if (< (count right) (count left))
     (intersection right left)
     (reduce
       (fn [result item]
         (if (contains? right item) result (disj result item)))
       left left)))
  ([left right & sets]
   (let [sets (bubble-max-key #(- (count %1))
                (conj sets right left))]
     (reduce intersection (first sets) (rest sets)))))

(defn difference
  ([left] left)
  ([left right]
   (if (< (count left) (count right))
     (reduce
       (fn [result item]
         (if (contains? right item) (disj result item) result))
       left left)
     (reduce disj left right)))
  ([left right & sets]
   (reduce difference left (conj sets right))))

(defn select [pred values]
  (reduce
    (fn [result item]
      (if (pred item) result (disj result item)))
    values values))

(defn project [relation keys]
  (with-meta
    (set (map #(select-keys %1 keys) relation))
    (meta relation)))

(defn rename-keys [map key-map]
  (reduce
    (fn [result [old-key new-key]]
      (if (contains? map old-key)
        (assoc result new-key (get map old-key))
        result))
    (apply dissoc map (keys key-map))
    key-map))

(defn rename [relation key-map]
  (with-meta
    (set (map #(rename-keys %1 key-map) relation))
    (meta relation)))

(defn index [relation keys]
  (reduce
    (fn [result item]
      (let [index-key (select-keys item keys)]
        (assoc result index-key
          (conj (get result index-key #{}) item))))
    {} relation))

(defn map-invert [map]
  (reduce-kv (fn [result key value]
               (assoc result value key))
    {} map))

(defn join
  ([left right]
   (if (and (seq left) (seq right))
     (let [keys (intersection
                  (set (keys (first left)))
                  (set (keys (first right))))
           [relation values] (if (<= (count left) (count right))
                               [left right]
                               [right left])
           indexed (index relation keys)]
       (reduce
         (fn [result item]
           (if-let [found (get indexed (select-keys item keys))]
             (reduce #(conj %1 (merge %2 item)) result found)
             result))
         #{} values))
     #{}))
  ([left right key-map]
   (let [[relation values keys]
         (if (<= (count left) (count right))
           [left right (map-invert key-map)]
           [right left key-map])
         indexed (index relation (vals keys))]
     (reduce
       (fn [result item]
         (let [index-key (rename-keys
                           (select-keys item (keys keys)) keys)]
           (if-let [found (get indexed index-key)]
             (reduce #(conj %1 (merge %2 item)) result found)
             result)))
       #{} values))))

(defn subset? [left right]
  (and (<= (count left) (count right))
    (every? #(contains? right %1) left)))

(defn superset? [left right]
  (and (>= (count left) (count right))
    (every? #(contains? left %1) right)))
