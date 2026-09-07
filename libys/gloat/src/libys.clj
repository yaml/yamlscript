(ns libys
  "Gloat EXPORT bridge for the libys C API."
  (:require
   [yamlscript.compiler :as compiler]
   [yamlscript.glojure-runtime :as runtime]
   [ys.v0.json :as json]))

(def EXPORT
  {"graal-create-isolate"     [:int :int :int :int]
   "graal-tear-down-isolate"  [:int :int]
   "graal-attach-thread"      [:int :int :int]
   "graal-detach-thread"      [:int :int]
   "graal-get-current-thread" [:int :int]
   "graal-get-isolate"        [:int :int]
   "load-ys-to-json"          [:int :str :str]})

;; Keep the GraalVM lifecycle ABI for existing bindings. Glojure uses a
;; process-wide Go runtime, so there is no isolate to create or destroy.
(defn graal-create-isolate [_params _isolate _thread] 0)
(defn graal-tear-down-isolate [_thread] 0)
(defn graal-attach-thread [_isolate _thread] 0)
(defn graal-detach-thread [_thread] 0)
(defn graal-get-current-thread [_isolate] 0)
(defn graal-get-isolate [_thread] 0)

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
