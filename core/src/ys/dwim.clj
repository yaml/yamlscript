;; Copyright 2023-2024 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.dwim
  (:require
   [clojure.string :refer [escape]]
   [yamlscript.util :refer [chop die]])
  (:refer-clojure :exclude [replace]))


;;------------------------------------------------------------------------------
(defn- regex? [x]
  (= (type x) java.util.regex.Pattern))

(defn- regex-to-fn [a b f]
  (let [a (if (regex? a) #(re-find a %1) a)]
    (f a b)))


;;------------------------------------------------------------------------------
(defn ++filter [a b] (regex-to-fn a b filter))

(defn ++filterv [a b] (regex-to-fn a b filterv))

(defn ++keep [a b] (regex-to-fn a b keep))

(defn ++map [a b]
  (if (and (ifn? b) (or (sequential? a) (string? a)))
    (map b a)
    (map a b)))

(defn ++mapv [a b]
  (if (and (ifn? b) (or (sequential? a) (string? a)))
    (mapv b a)
    (mapv a b)))

(defn ++remove [a b] (regex-to-fn a b remove))

(defn ++replace
  ([a b] (clojure.string/replace a b ""))
  ([a b c] (clojure.string/replace a b c)))

(defn ++take-while [a b] (regex-to-fn a b take-while))


;;------------------------------------------------------------------------------
(defmacro ^:private dwim [type idfn name]
  (let [dname (symbol (str "+" name))
        pname (symbol (str "++" name))
        name (if (resolve pname) pname name)]
    (cond
      (= 2.1 type)
      `(defn ~dname [x# y#]
         (if (~idfn y#)
           (~name y# x#)
           (~name x# y#)))
      (= 2.2 type)
      `(defn ~dname [x# y#]
         (if (~idfn x#)
           (~name y# x#)
           (~name x# y#)))
      (= 23.2 type)
      `(defn ~dname
         ([x# y#]
          (if (~idfn x#)
            (~name y# x#)
            (~name x# y#)))
         ([x# y# z#]
          (if (~idfn x#)
            (~name y# x# z#)
            (~name x# y# z#))))
      (= 9.1 type)
      `(defn ~dname [x# y# & xs#]
         (if (~idfn y#)
           (apply ~name y# x# xs#)
           (apply ~name x# y# xs#)))
      (= 9.9 type)
      `(defn ~dname [x# & xs#]
         (if (~idfn x#)
           (apply ~name (concat xs# [x#]))
           (apply ~name x# xs#)))
      :else (die "Bad dwim  type: " type))))


;;------------------------------------------------------------------------------
(dwim 2.2 seqable? apply)
(dwim 2.2 seqable? chop)
(dwim 2.2 seqable? cons)
(dwim 2.1 seqable? contains?)
(dwim 2.2 seqable? drop)
(dwim 2.2 seqable? drop-last)
(dwim 2.2 seqable? drop-while)
(dwim 2.2 map?     escape)
(dwim 2.2 seqable? every?)
(dwim 2.2 seqable? filter)
(dwim 2.2 seqable? filterv)
(dwim 9.1 string?  format)
(dwim 2.2 seqable? interpose)
(dwim 2.2 seqable? keep)
(dwim 2.2 seqable? map)
(dwim 2.2 seqable? mapcat)
(dwim 2.2 seqable? mapv)
(dwim 2.2 seqable? not-any?)
(dwim 2.1 seqable? nth)
(dwim 9.9 seqable? partition)
(dwim 2.2 seqable? random-sample)
(dwim 2.1 regex?   re-find)
(dwim 2.1 regex?   re-matches)
(dwim 2.1 regex?   re-seq)
(dwim 9.9 seqable? reduce)
(dwim 2.2 seqable? remove)
(dwim 2.2 seqable? repeat)
(dwim 23.2 regex?  replace)
(dwim 2.2 seqable? some)
(dwim 2.2 seqable? sort)
(dwim 2.2 seqable? split-at)
(dwim 2.2 seqable? split-with)
(dwim 2.2 seqable? take)
(dwim 2.2 seqable? take-last)
(dwim 2.2 seqable? take-while)

(comment
  )
