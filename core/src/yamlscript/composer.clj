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

(defn compose
  "Compose YAML parse events into a tree."
  ([events]
   (compose events {:first true, :last true, :init false}))
  ([events ctx]
   (let [node (or
                (->
                  events
                  compose-events
                  first)
                ;; Empty stream
                {:+ "code", := ""})
         ys-tag (:! node)
         node (dissoc node :!)
         node (case ys-tag
                "YS-v0" (assoc node :+ "code")
                "YS-v0:" (assoc node :+ "data")
                "yamlscript/v0" (assoc node :+ "code")
                "yamlscript/v0:" (assoc node :+ "data")
                "yamlscript/v0/" (assoc node :+ "data")  ; deprecated
                "yamlscript/v0/bare" (assoc node :+ "bare")
                "yamlscript/v0/code" (assoc node :+ "code")
                "yamlscript/v0/data" (assoc node :+ "data")
                (let [key1-tag (get-in node [:% 0 :!])
                      key1-val (get-in node [:% 0 :=])
                      val1 (get-in node [:% 1])]
                  (if (and
                        (= "YS" key1-tag)
                        (= "v0" key1-val))
                    (if (= {:= ""} val1)
                      (let [node (assoc-in node [:%]
                                   (vec (drop 2 (get-in node [:%]))))]
                        (merge {:+ "bare"} node))
                      ;; TODO Get YS config from value
                      (die "Values not yet supported for '!YS v0:' key"))
                    node)))

         node (if (and
                    (:+ node)
                    (= "" (:= node)))
                (assoc node :+ "code")
                node)

         mode (fn [mode]
                (when-not (:init ctx)
                  (die "YS tags not allowed w/o a YS tag at the top"))
                (assoc node :+ mode))

         node (if (not (:first ctx))
                (cond
                  (= "code" ys-tag) (mode "code")
                  (= "data" ys-tag) (mode "data")
                  (= "bare" ys-tag) (mode "bare")
                  (= "yaml" ys-tag) (mode "bare")
                  (= "yamlscript/v0" ys-tag) (mode "code")
                  (= "yamlscript/v0:" ys-tag) (mode "data")
                  (= "yamlscript/v0/data" ys-tag) (mode "data")
                  ys-tag (die "Invalid tag: !" ys-tag)
                  :else node)
                node)
         ctx (if (:+ node) (assoc ctx :init true) ctx)
         node (if (:+ node) node (assoc node :+ "bare"))
         node (if (= node {:+ "bare", :% []})
                {:+ "code", := ""}
                node)]
     [node ctx])))

(comment
  (YSC0 "
!YS-v0:
--- !code
x =: 1
y =: 2
--- !data
z:: x + y
")
  )

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

(comment)
  
