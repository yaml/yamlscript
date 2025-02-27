;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.composer is responsible for converting a stream of YAML
;; parse events into a tree of nodes.

(ns yamlscript.composer
  (:require
   [yamlscript.common])
  (:refer-clojure))

(comment
  [;
   :%  ; block mapping
   :-  ; block sequence
   :%% ; flow mapping
   :-- ; flow sequence

   :=  ; plain scalar
   :$  ; double quoted scalar
   :'  ; single quoted scalar
   :|  ; literal block scalar
   :>  ; folded block scalar

   :!  ; tag
   :&  ; anchor
   :*])  ; alias

(declare compose-events)

; !yamlscript/v0      (code)
; !yamlscript/v0:     (data)
; !yamlscript/v0/data
; !yamlscript/v0/code
; !yamlscript/v0/bare
; !YS-v0              (code)
; !YS-v0:             (data)
; !YS v0:             (bare)
; !YS-v0 data:        (data)
; !YS-v0 code:        (code)
; !YS-v0 bare:        (bare)
; !YS-v0 yaml:        (bare)

;; TODO:
;; * Support function call tags at top level: !yamlscript/v0/data;merge*:

(defn get-ys-tag [node]
  (if-let [tag (:! node)]
    (cond
      (or (re-find #"^YS-v0:?$" tag)
        (re-find #"^yamlscript/v0(?:/code|/data|/bare|:)?$" tag)
        (re-find #"^(?:code|data|bare)$" tag)) [tag (dissoc node :!)]
      (re-find #"^tag:yaml.org,2002:" tag) [nil node]
      :else (die "Invalid tag: !" tag))
    [nil node]))

(defn compose
  "Compose YAML parse events into a tree."
  ([events]
   (compose events {:first true, :last true, :init false}))
  ([events ctx]
   (let [node (or
                (-> events compose-events first)
                {:+ "code", := ""})  ;; Empty stream
         [ys-tag node] (get-ys-tag node)

         mode (fn
                ([mode] (assoc node :+ mode)))

         ys-pair? (fn [node]
                    (let [key1-tag (get-in node [:% 0 :!])
                          key1-val (get-in node [:% 0 :=])
                          val1 (get-in node [:% 1])]
                      (if (and (= "YS" key1-tag) (= "v0" key1-val))
                        (if (= {:= ""} val1)
                          true
                          (die "Values not yet supported for '!YS v0:' key"))
                        false)))

         node (if ys-tag
                (if-not (:init ctx)
                  (case ys-tag
                    "YS-v0" (mode "code")
                    "YS-v0:" (mode "data")
                    "yamlscript/v0" (mode "code")
                    "yamlscript/v0:" (mode "data")
                    "yamlscript/v0/bare" (mode "bare")
                    "yamlscript/v0/code" (mode "code")
                    "yamlscript/v0/data" (mode "data")
                    (die "Invalid tag: !" ys-tag))
                  (case ys-tag
                    "code" (mode "code")
                    "data" (mode "data")
                    "bare" (mode "bare")
                    "yaml" (mode "bare")
                    "YS-v0" (mode "code")
                    "YS-v0:" (mode "data")
                    "yamlscript/v0" (mode "code")
                    "yamlscript/v0:" (mode "data")
                    "yamlscript/v0/data" (mode "data")
                    (if ys-tag
                      (die "Invalid tag: !" ys-tag)
                      node)))
                (if (ys-pair? node)
                  (let [node (mode "bare")]
                    (assoc-in node [:%]
                      (vec (drop 2 (get-in node [:%])))))
                  node))

         ctx (if (re-find #"^(?:code|data|bare)$" (str (:+ node)))
               (assoc ctx :init true)
               ctx)
         node (if (:+ node) node (assoc node :+ "bare"))
         node (if (or
                    (= node {:+ "bare", :% []})
                    (= node {:+ "data", := ""}))
                {:+ "code", := ""}
                node)]
     [node ctx])))

(defn compose-mapping [events]
  (let [[event & events] events
        {anchor :& tag :! flow :flow} event
        {start :<} (meta event)
        mark (if flow :%% :%)]
    (loop [coll []
           events events]
      (let [event (first events)]
        (if (= "-MAP" (:+ event))
          (let [{end :>} (meta event)
                node {}
                node (if anchor (assoc node :& anchor) node)
                node (if tag (assoc node :! tag) node)
                node (assoc node mark coll)]
            [(with-meta
               node
               {:< start
                :> end})
             (rest events)])
          (let [[key events] (compose-events events)
                [val events] (compose-events events)]
            (recur (conj coll key val) events)))))))

(defn compose-sequence [events]
  (let [[event & events] events
        {anchor :& tag :! flow :flow} event
        {start :<} (meta event)
        mark (if flow :-- :-)]
    (loop [coll []
           events events]
      (let [event (first events)]
        (if (= "-SEQ" (:+ event))
          (let [{end :>} (meta event)
                node {}
                node (if anchor (assoc node :& anchor) node)
                node (if tag (assoc node :! tag) node)
                node (assoc node mark coll)]
            [(with-meta
               node
               {:< start
                :> end})
             (rest events)])
          (let [[elem events] (compose-events events)]
            (recur (conj coll elem) events)))))))

(defn compose-scalar [events]
  (let [[event & events] events
        {anchor :& tag :!} event
        key (some #{:= :$ :' :| :>} (keys event))
        {value key} event
        node {}
        node (if anchor (assoc node :& anchor) node)
        node (if tag (assoc node :! tag) node)
        node (assoc node key value)
        node (with-meta node (meta event))]
    [node events]))

(defn compose-alias [events]
  (let [[event & events] events
        {value :*} event
        node {:* value}
        node (with-meta node (meta event))]
    [node events]))

(defn compose-events [events]
  (case (:+ (first events))
    "+MAP" (compose-mapping events)
    "+SEQ" (compose-sequence events)
    "=VAL" (compose-scalar events)
    "=ALI" (compose-alias events)
    []))

(comment
  )
