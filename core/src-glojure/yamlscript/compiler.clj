;; Copyright 2023-2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns yamlscript.compiler
  (:require
   [clojure.string :as str]
   [yamlscript.builder :as builder]
   [yamlscript.composer]
   [yamlscript.constructor]
   [yamlscript.global :as global]
   [yamlscript.parser]
   [yamlscript.printer]
   [yamlscript.resolver]
   [yamlscript.transformer]
   [yamlscript.module.pprint :as pprint])
  (:refer-clojure :exclude [compile]))

(defn parse-events-to-groups [events]
  (->> events
    (reduce
      (fn [groups event]
        (if (= (:+ event) "+DOC")
          (conj groups [event])
          (update groups (dec (count groups)) conj event)))
      [[]])
    (map #(remove (fn [event] (= "DOC" (subs (:+ event) 1))) %))))

(defn- compile-events [events stage]
  (let [groups (parse-events-to-groups events)
        group-count (count groups)
        context {:first nil :last nil :init nil}]
    (loop [[events & remaining] groups
           context context
           blocks []
           index 1]
      (let [context (assoc context
                      :first (= index 1)
                      :last (>= index group-count))
            [node context] (stage "compose"
                             yamlscript.composer/compose
                             [events context])
            node (stage "resolve" yamlscript.resolver/resolve [node])
            node (stage "build" yamlscript.builder/build [node])
            node (stage "transform" yamlscript.transformer/transform [node])
            node (stage "construct" yamlscript.constructor/construct
                   [node context])
            block (stage "print" yamlscript.printer/print [node])
            blocks (conj blocks block)]
        (if (seq remaining)
          (recur remaining context blocks (inc index))
          (str/join "" blocks))))))

(defn compile [yamlscript-string]
  (reset! global/build-xstr builder/build-xstr)
  (compile-events
    (yamlscript.parser/parse yamlscript-string)
    (fn [_stage-name stage-fn input-args]
      (apply stage-fn input-args))))

(defn stage-with-options [stage-name stage-fn input-args]
  (let [start (time.Now)
        value (apply stage-fn input-args)
        elapsed (/ (double (.Nanoseconds (time.Since start))) 1000000.0)]
    (when (get-in @yamlscript.global/opts [:debug-stage stage-name])
      (println (fmt.Sprintf "*** %-9s *** %.6f ms" stage-name elapsed))
      (println)
      (pprint/pp value)
      (println))
    value))

(defn compile-with-options [yamlscript-string]
  (reset! global/build-xstr builder/build-xstr)
  (let [events (stage-with-options
                 "parse"
                 yamlscript.parser/parse
                 [yamlscript-string])]
    (compile-events events stage-with-options)))

(defn pretty-format [code]
  (->> (read-string (str "(do " code "\n)\n"))
    rest
    (map #(str (pprint/write %1 :stream nil) "\n"))
    (apply str)
    (#(str/replace %1 #"\r\n?" "\n"))))
