(ns libys
  "Evaluation implementation behind the native libys C API."
  (:require
   [yamlscript.compiler :as compiler]
   [yamlscript.glojure-runtime :as runtime]
   [ys.v0.json :as json]))

;; The handwritten native main package owns the public exports.
(def EXPORT {})

(def initialized? (atom false))

(defn initialize! []
  (when-not @initialized?
    (runtime/enter-main!)
    (reset! initialized? true)))

(defn load-ys-to-json
  "Compile and evaluate YAMLScript, returning the established JSON envelope."
  [_thread ys-str]
  (try
    (initialize!)
    (runtime/set-runtime! "NO-NAME" [] [])
    (json/dump
      {:data (runtime/eval-code (compiler/compile ys-str))})
    (catch go/any error
      (json/dump
        {:error
         {:cause (if (string? error)
                   error
                   (or (ex-message error) (str error)))
          :type (fmt.Sprintf "%T" error)}}))))
