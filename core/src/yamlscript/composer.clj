;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.composer is responsible for converting a stream of YAML
;; parse events into a tree of nodes.

(ns yamlscript.composer
  (:require
   [yamlscript.debug :refer [www]]))
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
   :*  ; alias
   ]
  )

(declare compose-events)

(defn compose
  "Compose YAML parse events into a tree."
  [events]
  (if (seq events)
    (->
      events
      compose-events
      first)
    {:! "yamlscript/v0", := ""}))

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
    "=ALI" (compose-alias events)))

(comment
  www
  )
