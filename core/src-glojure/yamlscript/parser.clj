;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.parser)

(def shebang-ys #"^#!.*/env ys-0(?:\.\d+\.\d+)?\r?\n")
(def shebang-bash #"^#!.*[/ ]bash\r?\n+source +<\(")

(defn- mark [[line column index]]
  [(dec line) (dec column) index])

(defn- event-meta [event]
  {:< (mark (:start event))
   :> (mark (:end event))})

(defn- normalize-tag [tag]
  (when tag
    (if (and (.startsWith tag "!")
             (not (.startsWith tag "tag:")))
      (subs tag 1)
      tag)))

(defn- normalize-event [event]
  (let [kind (:event event)
        name ({"document_start" "+DOC"
               "document_end" "-DOC"
               "mapping_start" "+MAP"
               "mapping_end" "-MAP"
               "sequence_start" "+SEQ"
               "sequence_end" "-SEQ"
               "scalar" "=VAL"
               "alias" "=ALI"} kind)
        value-key ({nil :=
                    "single" :'
                    "double" :$
                    "literal" :|
                    "folded" :>} (:style event))
        normalized (cond-> {:+ name}
                     (:flow event) (assoc :flow true)
                     (:anchor event) (assoc :& (:anchor event))
                     (:tag event) (assoc :! (normalize-tag (:tag event)))
                     (= kind "scalar")
                     (assoc value-key (:value event))
                     (= kind "alias") (assoc :* (:name event)))]
    (with-meta normalized (event-meta event))))

(defn parse [yaml-string]
  (let [[events error]
        (github.com:yaml:yamlscript:internal:goyamlparser.ParseYAMLScriptEvents
          yaml-string)]
    (when error (throw error))
    (let [events (map normalize-event (rest events))
          [first-event & rest-events] events
          shebang? (or (re-find shebang-ys yaml-string)
                       (re-find shebang-bash yaml-string))
          first-tag (:! first-event)
          first-event (if (and shebang?
                               (not (and first-tag
                                         (re-find #"^ys-0" first-tag))))
                        (assoc first-event :! "ys-0")
                        first-event)]
      (remove nil? (cons first-event rest-events)))))

(defn parse-test-case [yaml-string]
  (remove (fn [event] (= "DOC" (subs (:+ event) 1)))
          (parse yaml-string)))
