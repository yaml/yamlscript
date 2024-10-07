;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.parser is responsible for parsing YAML into a sequence of
;; event objects.

;; TODO
;; - switch from snakeyaml to libfyaml (ffi)

(ns yamlscript.parser
  (:require
   [yamlscript.common])
  (:import
   (java.util Optional)
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

(declare ys-event)

(def shebang-ys #"^#!.*/env ys-0\n")
(def shebang-bash #"^#!.*[/ ]bash\n+source +<\(")
(defn parse
  "Parse a YAML string into a sequence of event objects."
  [yaml-string]
  (let [parser (new Parse (.build (LoadSettings/builder)))
        has-code-mode-shebang (or (re-find shebang-ys yaml-string)
                                (re-find shebang-bash yaml-string))
        events (->> yaml-string
                 (.parseString parser)
                 (map ys-event)
                 (remove nil?)
                 rest)
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

(defn parse-test-case [yaml-string]
  (->> yaml-string
    parse
    (remove (fn [ev] (= "DOC" (subs (:+ ev) 1))))))

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

(defmulti  ys-event class)
(defmethod ys-event DocumentStartEvent [event] (doc-start  event))
(defmethod ys-event DocumentEndEvent   [event] (doc-end    event))
(defmethod ys-event MappingStartEvent  [event] (map-start  event))
(defmethod ys-event MappingEndEvent    [event] (map-end    event))
(defmethod ys-event SequenceStartEvent [event] (seq-start  event))
(defmethod ys-event SequenceEndEvent   [event] (seq-end    event))
(defmethod ys-event ScalarEvent        [event] (scalar-val event))
(defmethod ys-event AliasEvent         [event] (alias-val  event))
(defmethod ys-event :default [_] nil)

(comment
  (parse "a")
  )
