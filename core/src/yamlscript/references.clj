;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.references
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [yamlscript.ast :as ast :refer [Key Lst Sym Str Vec Nil]]
   [ys.v0.util :as util]))

(def ^:dynamic *anchors* nil)
(def ^:dynamic *used-anchors* #{})
(def ^:dynamic *document* 0)
(def ^:dynamic *document-result?* true)
(def ^:dynamic *stream-size-known?* true)

(defn known-stream-size [blocks]
  (reduce
   (fn [size block]
     (cond
       (str/blank? block) size
       (re-matches #"\(\+\+\+ (?:true|false)\)" block) size
       (or (re-find #"^\(\+\+\+ (?:\(%|\{|\[|\"|nil\)|[-+]?[0-9])" block)
           (re-find #"^\(\+\+\+ \(let .*\] __\)\)$" block)) (inc size)
       :else (reduced nil)))
   0 blocks))

(defn document-position [blocks]
  (or (known-stream-size blocks) (count (remove str/blank? blocks))))

(def special-calls
  {"#" 'count "?" 'truey? "!" 'falsey? "??" 'boolean "!!" 'not
   "--" 'dec+ "++" 'inc+ "@" 'deref ">" 'DBG ">>>" 'DBG})

(defn parse-reference
  "Parse the restricted expression-reference grammar."
  [source]
  (let [invalid #(util/die "Invalid reference syntax: " source)
        [kind name level offset body]
        (cond
          (str/starts-with? source "**")
          [:stream nil 0 nil (subs source 2)]
          (str/starts-with? source "*/")
          [:root nil 0 nil (str "." (subs source 2))]
          (str/starts-with? source "*^")
          (let [[_ carets depth sibling body]
                (re-matches #"\*(\^+)([0-9]+)?([+-][0-9]*)?(.*)" source)]
            [:container nil (if depth (parse-long depth) (count carets))
             sibling body])
          (re-find #"^\*[+-]" source)
          (let [[_ sibling body] (re-matches #"\*([+-][0-9]*)(.*)" source)]
            [:container nil 0 sibling body])
          (re-find #"^\*[.:]" source)
          [:container nil 0 nil (subs source 1)]
          :else
          (if-let [[_ name body]
                   (re-matches #"\*([A-Za-z0-9_][A-Za-z0-9_-]*)(.*)" source)]
            [:alias name 0 nil body]
            (invalid)))
        [path & calls] (str/split (or body "") #":" -1)
        _ (when-not (and
                     (or (empty? path) (= path ".")
                         (re-matches
                          #"(?:\.(?:[A-Za-z0-9_][A-Za-z0-9_-]*|[+-][0-9]+|\$|#|\?\??|!{1,2}|--|\+\+|@|>>>|>))+"
                          path))
                     (every? #(re-matches
                               #"[A-Za-z_][A-Za-z0-9_-]*\??" %1) calls))
            (invalid))
        parts (if (or (empty? path) (= path "."))
                [] (vec (str/split (subs path 1) #"\.")))]
    {:Ref {:source source :kind kind :name name :level level
           :offset (when offset
                     (if (#{"+" "-"} offset)
                       (if (= offset "+") 1 -1)
                       (parse-long offset)))
           :parts parts :calls calls}}))

(defn data? [node] (:yamlscript/data (meta node)))
(defn node-key [node]
  (cond
    (:Str node) (:Str node)
    (:Int node) (:Int node)
    (:Key node) (:Key node)
    :else (:Sym node)))

(defn map-pairs [node]
  (loop [nodes (or (:Map node) (:dmap node)) pairs []]
    (if (empty? nodes)
      pairs
      (if (vector? (first nodes))
        (recur (rest nodes) pairs)
        (recur (drop 2 nodes) (conj pairs (vec (take 2 nodes))))))))

(defn children [node]
  (if (map? node) (vals (dissoc node :& :!)) (when (coll? node) node)))

(defn reference? [node]
  (boolean (or (:Ref node) (:Ali node)
               (some reference? (children node)))))

(defn literal [value]
  (cond (map? value) value
        (integer? value) {:Int value}
        :else (Str value)))
(defn path-node [path] (Vec (mapv literal path)))
(def doc-symbol (Sym '__))
(def stream-node (Lst [(Sym 'stream)]))

(defn read-path [base path]
  (case (count path)
    0 base
    1 (Lst [(Sym (if (or (integer? (first path)) (map? (first path)))
                   'nth 'get))
            base (literal (first path))])
    (Lst [(Sym 'get-in) base (path-node path)])))

(declare lower-node lower-document)

(defn index-document [node]
  (let [schema (atom {}) anchors (atom {})]
    (letfn [(visit [node path]
              (when-let [name (:& node)]
                (swap! anchors assoc name
                       {:document *document* :path path
                        :position-known? *stream-size-known?*}))
              (when (data? node)
                (if (:Vec node)
                  (do
                    (swap! schema assoc path
                           {:kind :seq :size (count (:Vec node))
                            :dynamic? (boolean (some :dmap (:Vec node)))})
                    (doseq [[index child] (map-indexed vector (:Vec node))]
                      (visit child (conj path index))))
                  (let [pairs (map-pairs node)]
                    (swap! schema assoc path
                           {:kind :map :keys (mapv (comp node-key first) pairs)
                            :dynamic? (boolean (:dmap node))})
                    (doseq [[key value] pairs]
                      (visit value (conj path (node-key key))))))))]
      (visit node [])
      (when (and *anchors* *document-result?*)
        (swap! *anchors* merge @anchors))
      {:schema @schema :anchors @anchors})))

(defn sibling-path [path offset schema]
  (let [parent (vec (butlast path))
        {:keys [kind size keys dynamic?]} (get schema parent)
        key (peek path)]
    (if dynamic?
      {:dynamic {:parent parent :key key :node-path path :offset offset}}
      (let [index (if (= kind :seq) key
                      (first (keep-indexed #(when (= %2 key) %1) keys)))
            target (+ index offset)
            size (if (= kind :seq) size (count keys))]
        (if (and (>= target 0) (< target size))
          {:path (conj parent (if (= kind :seq) target (nth keys target)))}
          {:missing true})))))

(defn resolve-reference [ref {:keys [path containers schema anchors]}]
  (let [{:keys [kind name level offset source]} ref
        target
        (case kind
          :stream {:stream true :path []}
          :alias (or (get anchors name)
                     (when *anchors*
                       (let [anchor (get @*anchors* name)]
                         (when (and anchor
                                    (not= *document* (:document anchor)))
                           (if (:position-known? anchor) anchor
                               {:alias name}))))
                     {:alias name})
          :root (if containers {:path []}
                    (util/die "Reference requires a containing data collection: "
                              source))
          :container
          (let [index (- (count containers) level 1)]
            (when (neg? index)
              (util/die
               (if (seq containers)
                 "Reference exceeds available ancestor depth: "
                 "Reference requires a containing data collection: ")
               source))
            {:path (nth containers index)}))
        target (if (and offset (:path target))
                 (sibling-path
                  (vec (take (- (count path) level) path)) offset schema)
                 target)]
    target))

(defn actual-path [path {:keys [marks]}]
  (if-let [prefix (first
                   (sort-by count >
                            (filter #(= %1 (vec (take (count %1) path))) marks)))]
    (let [base (Lst [(Sym 'get) (Sym '__positions) (path-node prefix)])
          tail (drop (count prefix) path)]
      (if (seq tail)
        (Lst [(Sym 'into) base (path-node tail)]) base))
    (path-node path)))

(defn local-read [path ctx]
  (if (= (actual-path path ctx) (path-node path))
    (read-path doc-symbol path)
    (if (= 1 (count path))
      (Lst [(Sym (if (integer? (first path)) 'nth 'get)) doc-symbol
            (Lst [(Sym 'last) (actual-path path ctx)])])
      (Lst [(Sym 'get-in) doc-symbol (actual-path path ctx)]))))

(defn link-expression [value]
  (let [[function base key] (:Lst value)]
    (case (:Sym function)
      (get nth) (Lst [(Sym 'ys.v0.util/reference-link) base (Vec [key])])
      get-in (Lst [(Sym 'ys.v0.util/reference-link) base key])
      ys.v0.util/reference-sibling
      (update-in value [:Lst 0] (constantly (Sym 'ys.v0.util/reference-sibling-link)))
      (Lst [(Sym 'ys.v0.util/reference-link) value (Vec [])]))))

(defn plain-reference? [node]
  (let [ref (or (:Ref node)
                (when (:Ali node) (:Ref (parse-reference (str "*" (:Ali node))))))]
    (and ref (empty? (:calls ref))
         (not-any? #(or (= "$" %) (contains? special-calls %)) (:parts ref)))))

(defn lower-reference [ref ctx shape?]
  (let [target (resolve-reference ref ctx)
        {:keys [parts calls kind]} ref
        foreign? (and (:document target)
                      (not= (:document target) *document*))
        base (cond
               (:alias target)
               (Lst [(Sym '_**) (ast/Qts (:alias target))])
               (or (:stream target) foreign?) stream-node
               :else doc-symbol)
        start (if foreign?
                (into [(:document target)] (:path target))
                (:path target))
        result
        (loop [parts parts path start ops [] target target first? true]
          (if-let [part (first parts)]
            (cond
              (special-calls part)
              (recur (rest parts) path (conj ops (special-calls part))
                     target false)
              (= part "$")
              (if (and (= kind :container)
                       (= :seq (:kind (get (:schema ctx) path))))
                (recur (rest parts)
                       (conj path (dec (:size (get (:schema ctx) path))))
                       ops target false)
                (recur (rest parts) path (conj ops 'last) target false))
              :else
              (let [number (when (re-matches #"[+-]?[0-9]+" part)
                             (parse-long part))
                    relative? (and first? (= kind :container) number
                                   (re-find #"^[+-]" part))
                    index (cond
                            (and number (= kind :stream) (neg? number))
                            (if *stream-size-known?*
                              (+ *document* number)
                              (Lst [(Sym 'add+)
                                    (Lst [(Sym 'count) stream-node])
                                    {:Int number}]))
                            :else (or number part))]
                (if relative?
                  (let [target (sibling-path (:path ctx) number (:schema ctx))]
                    (recur (rest parts) (:path target) ops target false))
                  (recur (rest parts) (conj (vec path) index)
                         ops target false))))
            {:path path :ops ops :target target}))
        {:keys [path ops]} result
        target (:target result)
        dynamic (:dynamic target)
        value
        (cond
          (:missing target) (Nil)
          dynamic
          (read-path
           (Lst [(Sym 'ys.v0.util/reference-sibling)
                 (local-read (:parent dynamic) ctx)
                 (if (and (integer? (:key dynamic)) (seq (:marks ctx)))
                   (Lst [(Sym 'last) (actual-path (:node-path dynamic) ctx)])
                   (literal (:key dynamic)))
                 {:Int (:offset dynamic)}]) path)
          (or (:stream target) foreign?) (read-path base path)
          (:alias target)
          (reduce #(Lst [(Sym 'get+) %1
                         (if (integer? %2) (literal %2)
                             (ast/QSym (symbol %2)))]) base path)
          :else (local-read path ctx))
        shape? (and (or shape? (= 'count (first ops))
                        (and (empty? ops) (= "count" (first calls))))
                    (contains? (:schema ctx) path))
        _ (when (and (:reads ctx) (not shape?) path
                     (not foreign?) (not (:stream target))
                     (not (:missing target)))
            (swap! (:reads ctx) conj path))]
    (if (:link ctx)
      (link-expression value)
      (reduce #(Lst [(Sym %2) %1]) value
              (concat ops (map symbol calls))))))

(defn lower-node
  ([node ctx] (lower-node node ctx false))
  ([node ctx shape?]
   (cond
     (:Ref node) (lower-reference (:Ref node) ctx shape?)
     (:Ali node)
     (lower-node (parse-reference (str "*" (:Ali node))) ctx shape?)
     (map? node)
     (let [count? (= 'count (get-in node [:Lst 0 :Sym]))]
       (with-meta
         (reduce-kv
          (fn [node key value]
            (assoc node key
                   (cond
                     (#{:& :!} key) value
                     (and count? (= key :Lst))
                     (mapv #(lower-node %2 ctx (= %1 1)) (range) value)
                     :else (lower-node value ctx))))
          (empty node) node)
         (meta node)))
     (vector? node) (mapv #(lower-node %1 ctx) node)
     (seq? node) (doall (map #(lower-node %1 ctx) node))
     :else node)))

(defn cycle-path [tasks remaining]
  (loop [todo (mapv #(vector %1 [] #{}) (reverse (sort remaining)))]
    (let [[id path active] (peek todo) todo (pop todo)]
      (if (contains? active id)
        (conj (vec (drop-while #(not= id %1) path)) id)
        (recur
         (into todo
               (map #(vector %1 (conj path id) (conj active id))
                    (reverse (sort (set/intersection remaining
                                                     (:deps (nth tasks id))))))))))))

(defn dependency-order
  ([tasks] (dependency-order tasks true))
  ([tasks fail?]
   (loop [remaining (set (range (count tasks))) done #{} order []]
     (if (empty? remaining)
       order
       (if-let [ready (first (filter
                              #(set/subset? (:deps (nth tasks %1)) done)
                              (sort remaining)))]
         (recur (disj remaining ready) (conj done ready) (conj order ready))
         (when fail?
           (let [cycle (cycle-path tasks remaining)]
             (util/die "Circular deferred dependency: "
                       (str/join " -> "
                                 (map #(str "/" (str/join "/" (:path (nth tasks %1))))
                                      cycle))))))))))

(defn lower-document [node]
  (let [{:keys [schema anchors]} (index-document node)
        tasks (atom []) marks (atom #{})
        active? (reference? node)
        ctx {:schema schema :anchors anchors :path [] :containers []}
        strip? (or active? (and *document-result?* *stream-size-known?*
                                (some *used-anchors* (keys anchors))))]
    (letfn [(collect [node path containers]
              (let [ctx (assoc ctx :path path :containers containers)
                    node (if strip? (dissoc node :&) node)]
                (cond
                  (and (data? node) (:Vec node))
                  (let [dynamic? (:dynamic? (get schema path))
                        containers (conj containers path)]
                    (assoc node :Vec
                           (mapv
                            (fn [index child]
                              (let [child-path (conj path index)
                                    child (collect child child-path containers)]
                                (if (and active? dynamic? (not (:dmap child)))
                                  (do (swap! marks conj child-path)
                                      (Lst [(Sym 'ys.v0.util/reference-mark)
                                            (path-node child-path) child]))
                                  child)))
                            (range) (:Vec node))))
                  (and (data? node) (or (:Map node) (:dmap node)))
                  (let [containers (conj containers path)
                        body (or (:Map node) (:dmap node))
                        body
                        (loop [body body new []]
                          (cond
                            (empty? body) new
                            (vector? (first body))
                            (recur (rest body)
                                   (conj new (lower-node (first body) ctx)))
                            (= :-<< (node-key (first body)))
                            (let [value (second body)]
                              (swap! tasks conj
                                     {:path path :containers containers
                                      :merge true :value value})
                              (recur (drop 2 body) new))
                            :else
                            (let [[key value] body
                                  value (collect value
                                                 (conj path (node-key key)) containers)]
                              (recur (drop 2 body) (conj new key value)))))]
                    (cond-> (assoc node (if (:Map node) :Map :dmap) body)
                      (= "+merge" (:! node)) (dissoc :!)))
                  (reference? node)
                  (do (swap! tasks conj
                             {:path path :containers containers :value node})
                      (Nil))
                  :else node)))]
      (let [base (if active? (collect node [] [])
                     (if strip?
                       (letfn [(strip [node]
                                 (if (map? node)
                                   (with-meta
                                     (into {} (map (fn [[k v]] [k (strip v)])
                                                   (dissoc node :&)))
                                     (meta node))
                                   (if (vector? node) (mapv strip node) node)))]
                         (strip node))
                       node))
            tasks
            (mapv
             (fn [task]
               (let [reads (atom [])
                     value (lower-node (:value task)
                                       (assoc ctx :path (:path task)
                                              :containers (:containers task)
                                              :marks @marks :reads reads))]
                 (assoc task :source (:value task) :value value :reads @reads))) @tasks)
            tasks (mapv
                   (fn [task]
                     (assoc task :deps
                            (set (keep-indexed
                                  (fn [index other]
                                    (when (some
                                           #(or
                                             (= %1 (vec (take (count %1)
                                                              (:path other))))
                                             (= (:path other)
                                                (vec (take (count (:path other))
                                                           %1))))
                                           (:reads task)) index)) tasks))))
                   tasks)
            runtime? (some (fn [task]
                             (and (not (:merge task))
                                  (not (plain-reference? (:source task)))
                                  (some
                                   (fn [path]
                                     (and (contains? schema path)
                                          (some #(= path (vec (take (count path)
                                                                    (:path %))))
                                                tasks)))
                                   (:reads task))))
                           tasks)
            order (when-not runtime? (dependency-order tasks false))
            bindings
            (mapcat
             (fn [index]
               (let [{:keys [path value merge]} (nth tasks index)
                     dynamic? (not= (actual-path path {:marks @marks})
                                    (path-node path))
                     single? (= 1 (count path))
                     key (if single?
                           (if dynamic?
                             (Lst [(Sym 'last)
                                   (actual-path path {:marks @marks})])
                             (literal (first path)))
                           (actual-path path {:marks @marks}))
                     value (if merge
                             (Lst [(Sym 'fn) (Vec [(Sym 'value)])
                                   (Lst [(Sym 'merge-omap) (Sym 'value)
                                         value (Sym 'value)])]) value)
                     form (if (empty? path)
                            (if merge (Lst [value doc-symbol]) value)
                            (Lst [(Sym (if merge
                                         (if single? 'update 'update-in)
                                         (if single? 'assoc 'assoc-in)))
                                  doc-symbol key value]))]
                 [doc-symbol form]))
             order)
            ;; An arbitrary code document may or may not join the stream.
            ;; When its index is unknown, keep completed anchor snapshots
            ;; for later documents rather than guessing an absolute index.
            anchors (when (and active?
                               (or (not *document-result?*)
                                   (not *stream-size-known?*)))
                      (mapcat
                       (fn [[name anchor]]
                         [(Sym '_)
                          (Lst [(Sym '_&)
                                ((if (re-matches
                                      #"[A-Za-z0-9_][A-Za-z0-9_-]*" name)
                                   ast/Qts Str) name)
                                (local-read (:path anchor) {:marks @marks})])])
                       anchors))
            initial (if (seq @marks)
                      [(Vec [doc-symbol (Sym '__positions)])
                       (Lst [(Sym 'ys.v0.util/reference-positions) base])]
                      [doc-symbol base])]
        (cond
          (empty? tasks) base
          (some? order)
          (Lst [(Sym 'let) (Vec (concat initial bindings anchors)) doc-symbol])
          :else
          (let [evaluators
                (mapv
                 (fn [task]
                   (let [alias? (and (not (:merge task))
                                     (plain-reference? (:source task)))
                         value (lower-node (:source task)
                                           (assoc ctx :path (:path task)
                                                  :containers (:containers task)
                                                  :marks @marks :link alias?))
                         value (if (:merge task)
                                 (let [local (read-path (Sym '__base) (:path task))]
                                   (Lst [(Sym 'merge-omap) local value local]))
                                 value)]
                     (ast/MapNode
                      (concat
                       [(Key :path) (actual-path (:path task) {:marks @marks})]
                       (when alias? [(Key :alias) {:Bln true}])
                       [(Key :eval)
                        (Lst [(Sym 'fn) (Vec [doc-symbol]) value])])))) tasks)]
            (Lst [(Sym 'let)
                  (Vec (concat initial [(Sym '__base) doc-symbol]
                               [doc-symbol
                                (Lst [(Sym 'ys.v0.util/reference-document)
                                      doc-symbol (Vec evaluators)])]
                               anchors))
                  doc-symbol])))))))

(defn transform
  ([node] (transform node true))
  ([node result?]
   (cond
     (data? node) (binding [*document-result?* result?]
                    (lower-document node))
     (:xmap node)
     (assoc node :xmap
            (vec (mapcat
                  (fn [[key value]]
                    [(transform key false)
                     (transform value (and result? (= '=> (:Sym key))))])
                  (partition 2 (:xmap node)))))
     (:Ref node) (lower-node node {})
     (:Ali node) (lower-node node {})
     (map? node)
     (with-meta (reduce-kv #(assoc %1 %2
                                   (if (#{:& :!} %2) %3 (transform %3 false)))
                           (empty node) node) (meta node))
     (vector? node) (mapv #(transform %1 false) node)
     (seq? node) (doall (map #(transform %1 false) node))
     :else node)))
