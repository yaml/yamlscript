;; Copyright 2023-2025 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.parser is responsible for parsing YAML into a sequence of
;; event objects.

(ns yamlscript.parser
  (:require
   [clojure.string :as str]
   [yamlscript.common])
  (:import
   (java.util Optional)
   (java.nio.charset StandardCharsets)
   (org.rapidyaml Evt Rapidyaml)
   (org.snakeyaml.engine.v2.api LoadSettings)
   (org.snakeyaml.engine.v2.api.lowlevel Parse)
   (org.snakeyaml.engine.v2.exceptions Mark)
   (org.snakeyaml.engine.v2.events
     Event
     NodeEvent
     AliasEvent
     ScalarEvent
     CollectionStartEvent
     DocumentStartEvent
     DocumentEndEvent
     MappingStartEvent
     MappingEndEvent
     SequenceStartEvent
     SequenceEndEvent))
  (:refer-clojure))

(declare parse-fn)

(def shebang-ys #"^#!.*/env ys-0(?:\.d+\.\d+)?\n")
(def shebang-bash #"^#!.*[/ ]bash\n+source +<\(")
(defn parse
  "Parse a YAML string into a sequence of event objects."
  [yaml-string]
  (let [has-code-mode-shebang (or
                                (re-find shebang-ys yaml-string)
                                (re-find shebang-bash yaml-string))
        events (if (System/getenv "YS_PARSER_TIME")
                 (time (parse-fn yaml-string))
                 (parse-fn yaml-string))
        [first-event & rest-events] events
        first-event-tag (:! first-event)
        first-event (if (and has-code-mode-shebang
                          (not (and first-event-tag
                                 (re-find
                                   #"^yamlscript/v0"
                                   first-event-tag))))
                      (assoc first-event :! "yamlscript/v0")
                      first-event)
        events (cons first-event rest-events)]
    (remove nil? events)))

(declare snake-event)

;;
;; SnakeYAML Parser
;;

;; TODO - Set bigger buffer size in scanner class
(defn parse-snakeyaml [yaml-string]
  (let [parser (new Parse (.build (LoadSettings/builder)))]
    (->> yaml-string
      (.parseString parser)
      (map snake-event)
      (remove nil?)
      rest)))

;;
;; Functions to turn Java event objects into Clojure objects
;;
(defn event-obj [^Event event]
  (let [class (class event)
        name (condp = class
               DocumentStartEvent "+DOC"
               DocumentEndEvent "-DOC"
               MappingStartEvent "+MAP"
               MappingEndEvent "-MAP"
               SequenceStartEvent "+SEQ"
               SequenceEndEvent "-SEQ"
               ScalarEvent "=VAL"
               AliasEvent "=ALI"
               (die class))
        start ^Optional (. event getStartMark)
        end ^Optional (. event getEndMark)]
    (with-meta
      {:+ name}
      {:<
       [(. ^Mark (.orElse start nil) getLine)
        (. ^Mark (.orElse start nil) getColumn)
        (. ^Mark (.orElse start nil) getIndex)]
       :>
       [(. ^Mark (.orElse end nil) getLine)
        (. ^Mark (.orElse end nil) getColumn)
        (. ^Mark (.orElse end nil) getIndex)]})))

(defn event-start [event]
  (let [obj (event-obj event)
        doc-event? (some #(instance? % event)
                     [DocumentStartEvent DocumentEndEvent])
        coll-start? (some #(instance? % event)
                      [MappingStartEvent SequenceStartEvent])
        flow (when coll-start?
               (. ^CollectionStartEvent event isFlow))
        anchor (when-not doc-event?
                 (str (.orElse ^Optional (. ^NodeEvent event getAnchor) nil)))
        tag (if (= ScalarEvent (class event))
              (str (.orElse ^Optional (. ^ScalarEvent event getTag) nil))
              (when (not doc-event?)
                (str (.orElse ^Optional (. ^CollectionStartEvent event getTag)
                       nil))))
        obj (if flow (assoc obj :flow true) obj)
        obj (if (= "" anchor) obj (assoc obj :& anchor))
        obj (if (or (nil? tag) (= "" tag))
              obj
              (if (re-find #"^tag:" tag)
                (assoc obj :! tag)
                (assoc obj :! (subs tag 1))))]
    obj))

(defn doc-start [^DocumentStartEvent event] (event-start event))
(defn doc-end   [^DocumentEndEvent event]   (event-obj event))
(defn map-start [^MappingStartEvent event]  (event-start event))
(defn map-end   [^MappingEndEvent event]    (event-obj event))
(defn seq-start [^SequenceStartEvent event] (event-start event))
(defn seq-end   [^SequenceEndEvent event]   (event-obj event))
(defn scalar-val [^ScalarEvent event]
  (let [obj (event-start event)
        style (.. event (getScalarStyle) (toString))
        style (condp = style
                ":" "="
                "\"" "$"
                style)
        style (keyword style)]
    (assoc obj style (. event getValue))))
(defn alias-val [^AliasEvent event]
  (let [obj (event-obj event)]
    (assoc obj :* (str (. event getAlias)))))

(defmulti  snake-event class)
(defmethod snake-event DocumentStartEvent [event] (doc-start  event))
(defmethod snake-event DocumentEndEvent   [event] (doc-end    event))
(defmethod snake-event MappingStartEvent  [event] (map-start  event))
(defmethod snake-event MappingEndEvent    [event] (map-end    event))
(defmethod snake-event SequenceStartEvent [event] (seq-start  event))
(defmethod snake-event SequenceEndEvent   [event] (seq-end    event))
(defmethod snake-event ScalarEvent        [event] (scalar-val event))
(defmethod snake-event AliasEvent         [event] (alias-val  event))
(defmethod snake-event :default [_] nil)

;;
;; RapidYAML Parser
;;

(defn event-type [mask]
  (condp = (bit-and mask 2r11111111111)
    Evt/BSTR nil
    Evt/ESTR nil
    Evt/BDOC "+DOC"
    Evt/EDOC "-DOC"
    Evt/BMAP "+MAP"
    Evt/EMAP "-MAP"
    Evt/BSEQ "+SEQ"
    Evt/ESEQ "-SEQ"
    Evt/SCLR "=VAL"
    Evt/ALIA "=ALI"
    nil))

(defmacro flag? [flag mask]
  `(pos? (bit-and ~mask (. Evt ~flag))))

(defn get-skey [mask]
  (condp = (bit-and mask 2r111110000000000000000)
    Evt/PLAI :=
    Evt/SQUO :'
    Evt/DQUO :$
    Evt/LITL :|
    Evt/FOLD :>
    nil))

(defn parse-rapidyaml [^String yaml-string]
  (rest
    (let [parser ^Rapidyaml (new Rapidyaml)
          buffer (.getBytes yaml-string StandardCharsets/UTF_8)
          masks (int-array 5)
          needed (.parseYsToEvt parser buffer masks)
          buffer (.getBytes yaml-string StandardCharsets/UTF_8)
          masks (int-array needed)
          _ (.parseYsToEvt parser buffer masks)
          get-str (fn [i]
                    (let [off (aget masks (inc i))
                          len (aget masks (+ i 2))]
                      (reduce
                        (fn [slice i] (str slice (char (aget buffer i))))
                        "" (range off (+ off len)))))]

      (loop [i 0, tag nil, anchor nil, events []]
        (if (< i needed)
          (let [mask (aget masks i)
                type (event-type mask)
                ; _ (WWW (Integer/toString mask 2) type)
                sval (when (flag? HAS_STR mask) (get-str i))
                tag (if (flag? TAG_ mask) sval tag)
                anchor (if (flag? ANCH mask) sval anchor)
                event (when type
                        (let [event {:+ type}
                              event (if (flag? FLOW mask)
                                      (assoc event :flow true) event)
                              event (if anchor (assoc event :& anchor) event)
                              event (if tag
                                      (let [tag (str/replace tag
                                                  #"^!!"
                                                  "tag:yaml.org,2002:")]
                                        (assoc event :! tag)) event)
                              event (if sval (assoc event
                                               (get-skey mask) sval) event)
                              event (if (= type "=ALI")
                                      {:+ "=ALI" :* sval}
                                      event)]
                          event))
                events (if event (conj events event) events)
                i (+ i (if sval 3 1))]
            (if event
              (recur i nil nil events)
              (recur i tag anchor events)))
          events)))))

(def parse-fn (if-let [parser-name (System/getenv "YS_PARSER")]
                (condp = parser-name
                  "" parse-snakeyaml
                  "snake" parse-snakeyaml
                  "rapid" parse-rapidyaml
                  "ryml" parse-rapidyaml
                  ; TODO:
                  ;"rapid-buf" parse-rapidyaml-buf
                  ;"ryml-buf" parse-rapidyaml-buf
                  (die "Unknown YS_PARSER value: " parser-name))
                parse-snakeyaml))

(comment
  )
