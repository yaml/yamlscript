(load "src/yamlscript.lisp")

(let ((json (yamlscript:load-json
              (format nil "!ys-0:~%test:: inc(41)"))))
  (unless (search "\"test\":42" json)
    (error "load ys code failed"))
  (format t "ok - load ys code~%"))
