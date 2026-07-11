(defpackage #:yamlscript
  (:use #:cl)
  (:export #:load-json))

(in-package #:yamlscript)

(defun load-json (input)
  (with-input-from-string (in input)
    (with-output-to-string (out)
      (let ((proc (sb-ext:run-program
                    "./bin/yamlscript-lisp-json"
                    nil
                    :input in
                    :output out
                    :error *error-output*
                    :wait t)))
        (unless (zerop (sb-ext:process-exit-code proc))
          (error "yamlscript helper failed"))))))
