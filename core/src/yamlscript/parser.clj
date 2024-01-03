;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.parser is responsible for parsing YAML into a sequence of
;; event objects.

;; TODO
;; - switch from snakeyaml to libfyaml (ffi)

(ns yamlscript.parser
  (:use yamlscript.debug)
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
     MappingStartEvent
     MappingEndEvent
     SequenceStartEvent
     SequenceEndEvent)))

(declare ys-event)

(defn parse
  "Parse a YAML string into a sequence of event objects."
  [yaml-string]
  (let [parser (new Parse (.build (LoadSettings/builder)))
        has-code-mode-shebang (re-find
                                  #"^#!.*ys-0"
                                  yaml-string)
        events (->> yaml-string
                 (.parseString parser)
                 (map ys-event)
                 (remove nil?))
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

;;
;; Functions to turn Java event objects into Clojure objects
;;
(defn event-obj [^Event event]
  (let [class (class event)
        name (cond (= class MappingStartEvent) "+MAP"
                   (= class MappingEndEvent) "-MAP"
                   (= class SequenceStartEvent) "+SEQ"
                   (= class SequenceEndEvent) "-SEQ"
                   (= class ScalarEvent) "=VAL"
                   (= class AliasEvent) "=ALI"
                   :else (throw (Exception. (str class))))
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
        flow (when (not= ScalarEvent (class event))
               (. ^CollectionStartEvent event isFlow))
        anchor (str (.orElse ^Optional (. ^NodeEvent event getAnchor) nil))
        tag (if (= ScalarEvent (class event))
              (str (.orElse ^Optional (. ^ScalarEvent event getTag) nil))
              (str (.orElse ^Optional (. ^CollectionStartEvent event getTag)
                     nil)))
        obj (if flow (assoc obj :flow true) obj)
        obj (if (= "" anchor) obj (assoc obj :& anchor))
        obj (if (= "" tag)
              obj
              (if (re-find #"^tag:" tag)
                (assoc obj :! tag )
                (assoc obj :! (subs tag 1))
                ))]
    obj))

(defn map-start [^MappingStartEvent event] (event-start event))
(defn map-end [^MappingEndEvent event] (event-obj event))
(defn seq-start [^SequenceStartEvent event] (event-start event))
(defn seq-end [^SequenceEndEvent event] (event-obj event))
(defn scalar-val [^ScalarEvent event]
  (let [obj (event-start event)
        style (.. event (getScalarStyle) (toString))
        style (cond (= style ":") "="
                    (= style "\"") "$"
                    :else style)
        style (keyword style)]
    (assoc obj style (. event getValue))))
(defn alias-val [^AliasEvent event]
  (let [obj (event-obj event)]
    (assoc obj :* (str (. event getAlias)))))

(defmulti  ys-event class)
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
