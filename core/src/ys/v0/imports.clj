;; Copyright 2026 Ingy dot Net
;; This code is licensed under MIT license (See License for details)

(ns ys.v0.imports
  (:require [clojure.string :as str]
            [ys.v0.util :as util]))

(defn short-module? [module]
  "Return true for an unqualified, undotted module symbol."
  (and (symbol? module)
    (nil? (namespace module))
    (not (str/includes? (str module) "."))))

(defn with-short-from-alias [module options]
  "Use a short :from module name as its default alias."
  (if (and (short-module? module)
        (= :from (first (:source options)))
        (nil? (:as options)))
    (assoc options :as module)
    options))

(defn v0-imports
  "Alias available, permitted standard modules without replacing aliases."
  [available aliases allowed]
  (for [module (sort available)
          :let [alias (symbol (subs (str module) 3))]
          :when (and (contains? available module)
                  (or (nil? allowed) (contains? allowed module))
                  (not (contains? aliases alias)))]
      (with-meta (list module :as alias) {:umbrella true})))

(defn expression-imports?
  "Let leading namespace and import declarations establish their aliases."
  [form]
  (if (and (seq? form) (contains? '#{+++ TTT} (first form)))
    (expression-imports? (second form))
    (not (and (seq? form) (contains? '#{ns use} (first form))))))

(defn normalize-use-forms
  ([forms]
   (normalize-use-forms
     forms @(util/backend 'ys.v0.manifest/modules)))
  ([forms public-modules]
   (let [forms (if (every? symbol? forms)
                 (map list forms)
                 (if (symbol? (first forms)) (list forms) forms))]
     (map
       (fn [form]
         (let [[module & args] form]
           (cond
             (contains? '#{v0 ys.v0} module)
             (do
               (when (seq args)
                 (util/die
                   "The v0 umbrella import does not accept modifiers"))
               (list 'ys.v0))
             (short-module? module)
             (let [public-module (symbol (str "ys." module))]
               (if (contains? public-modules public-module)
                 (if (seq args)
                   (cons public-module args)
                   (list public-module :as module))
                 form))
             :else form)))
       forms))))
