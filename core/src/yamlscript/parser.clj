;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

;; The yamlscript.parser is responsible for parsing YAML into a sequence of
;; event objects.

;; TODO
;; - switch from snakeyaml to libfyaml (ffi)

(ns yamlscript.parser
  (:require
   [clojure.string :as str]
   [yamlscript.util :refer [die]])
  (:import
   (java.util Optional)
   (org.rapidyaml Rapidyaml)
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
     SequenceEndEvent)))

(declare
  parse-snakeyaml
  parse-rapidyaml)

(defn parse
  "Parse a YAML string into a sequence of event objects."
  [yaml-string]
  (let [has-code-mode-shebang (re-find
                                #"^#!.*ys-0"
                                yaml-string)
        events (if (System/getenv "YS_PARSER_RAPIDYAML")
                 (parse-rapidyaml yaml-string)
                 (parse-snakeyaml yaml-string))
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

;; TODO - Set bigger buffer size in scanner class
(defn parse-snakeyaml [yaml-string]
  (let [parser (new Parse (.build (LoadSettings/builder)))]
    (->> yaml-string
      (.parseString parser)
      (map snake-event)
      (remove nil?)
      rest)))

(defn parse-rapidyaml [yaml-string]
  (let [rapid-parser (new Rapidyaml)]
    (->> yaml-string
      ( #(.parseYS ^Rapidyaml rapid-parser %1))
      #_(#(do (println %1) %1))
      (#(str/replace %1 #"^\(\n\{:\+\ \"\+DOC\"\}" "("))
      #_(#(do (println %1) %1))
      read-string)))

(defn parse-test-case [yaml-string]
  (->> yaml-string
    parse
    (remove (fn [ev] (= "DOC" (subs (:+ ev) 1))))))

;;
;; Functions to turn Java event objects into Clojure objects
;;
(defn event-obj [^Event event]
  (let [class (class event)
        name (cond
               (= class DocumentStartEvent) "+DOC"
               (= class DocumentEndEvent) "-DOC"
               (= class MappingStartEvent) "+MAP"
               (= class MappingEndEvent) "-MAP"
               (= class SequenceStartEvent) "+SEQ"
               (= class SequenceEndEvent) "-SEQ"
               (= class ScalarEvent) "=VAL"
               (= class AliasEvent) "=ALI"
               :else (die class))
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
        style (cond (= style ":") "="
                    (= style "\"") "$"
                    :else style)
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

(def unescapes
  {"\\\\" "\\"
   "\\n" "\n"
   "\\t" "\t"
   "\\\"" "\""})

(defn str-unescape [s]
  (str/replace s #"(?:\\\\|\\n|\\t|\\\")"
    (fn [m] (get unescapes m))))

(comment
  (parse "a")
  )
