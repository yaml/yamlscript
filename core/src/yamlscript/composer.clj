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

; !ys-0               (code)
; !ys-0:              (data)
; !yaml               (bare)
; !YS-v0              (code)
; !YS-v0:             (data)
; !yamlscript/v0      (code)
; !yamlscript/v0:     (data)

;; TODO:
;; * Support function call tags at top level: !ys-0:first

(defn get-ys-tag
  "Get the YS tag from a top level node. Currently (v0) there are a few
  different variations of the YS tag that are supported. For v1 we'll likely
  only support !ys-0 !ys-0: !ys-1 !ys-1:. The !ys-0 form allows 0-n function
  call tags to follow it: !ys-0:reverse:rest for example."
  [node]
  (if-let [tag (:! node)]
    (if-let [[_ ystag fntag colon?] (re-matches #"(ys(?:-0)?)(:.+?)(:?)" tag)]
      (let [ystag (if-not (empty? colon?) ystag (str ystag ":"))
            node (assoc node :! fntag)]
        [ystag node])
      (cond
        (or
          (re-matches #"ys:?" tag)
          (re-matches #"ys-0:?" tag)
          (re-matches #"YS-v0:?" tag)
          (re-matches #"(?:bare|data|code)" tag)
          (re-matches #"yamlscript/v0(?::|/bare|/data|/code)?" tag))
        [tag (dissoc node :!)]
        ,
        (re-find #"^tag:yaml.org,2002:" tag) [nil node]
        :else (die "Invalid tag: !" tag)))
    [nil node]))

(comment
  (get-ys-tag {:! "yamlscript/v0/data"
               :- [{:= "foo"}]})
  )

(defn compose
  "Compose YAML parse events into an initial tree form."
  ([events]
   (let [ctx {:init false,  ;; Initial !ys-0 tag has been seen
              :first true,  ;; First document node in stream
              :last true,   ;; Last document node in stream
              }]
     (compose events ctx)))
  ([events ctx]
   (let [node (or
                (-> events compose-events first)
                {:+ "code", := ""})  ;; Support empty stream (no events)

         ;; Get the YS tag from the node
         [ys-tag node] (get-ys-tag node)

         mode (fn [mode] (assoc node :+ mode))

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
                  ;; First ys tag must be one of these:
                  (case ys-tag
                    "ys-0" (mode "code")
                    "ys-0:" (mode "data")
                    ;; The forms below are deprecated:
                    "YS-v0" (mode "code")
                    "YS-v0:" (mode "data")
                    "yamlscript/v0" (mode "code")
                    "yamlscript/v0:" (mode "data")
                    "yamlscript/v0/bare" (mode "bare")
                    "yamlscript/v0/data" (mode "data")
                    "yamlscript/v0/code" (mode "code")
                    (die "Invalid tag: '!" ys-tag "'\n"
                      "First ys tag must be one of these: "
                      "'ys-0', 'ys-0:', 'YS-v0', 'YS-v0:'"))
                  ;; Subsequent ys tags must be one of these:
                  (case ys-tag
                    "ys" (mode "code")
                    "ys:" (mode "data")
                    "yaml" (mode "bare")
                    ;; These 3 are deprecated:
                    "bare" (mode "bare")
                    "data" (mode "data")
                    "code" (mode "code")
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

(defn compose-mapping
  "Compose a YAML mapping event range into a mapping node."
  [events]
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

(defn compose-sequence
  "Compose a YAML sequence event range into a sequence node."
  [events]
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

(defn compose-scalar
  "Compose a YAML scalar event into a scalar node with style metadata."
  [events]
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

(defn compose-alias
  "Compose a YAML alias event into an alias node."
  [events]
  (let [[event & events] events
        {value :*} event
        node {:* value}
        node (with-meta node (meta event))]
    [node events]))

(defn compose-events
  "Dispatch the next parser event to the matching compose function."
  [events]
  (case (:+ (first events))
    "+MAP" (compose-mapping events)
    "+SEQ" (compose-sequence events)
    "=VAL" (compose-scalar events)
    "=ALI" (compose-alias events)
    []))

(comment
  )
