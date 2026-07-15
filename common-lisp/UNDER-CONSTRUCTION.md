# Common Lisp Binding Under Construction

The YAMLScript Common Lisp binding is currently experimental and not wired into
the release process.

The implementation in this directory is SBCL-specific.
It shells out to the `bin/yamlscript-lisp-json` helper program from
`src/yamlscript.lisp` rather than loading the YAMLScript native library
directly through a portable Common Lisp FFI layer.

The current `make test` target is intentionally skipped because the helper
process currently crashes when using `libys`.
That needs to be fixed before the binding should be published.

Common Lisp publishing is also different from most YAMLScript bindings.
Quicklisp is the most common distribution channel, but it is an index/request
workflow rather than a token-based upload registry.
Ultralisp is faster and can track a public project from GitHub or another
forge, but it is still a distribution index rather than a normal package
upload.

The likely publishing path is:

* Fix the helper process crash and make `make test` run real tests.
* Decide whether the binding should remain SBCL-specific or use a portable FFI.
* Publish from a public repository that Common Lisp package indexes can track.
* Add the project to Ultralisp first for faster availability.
* Request Quicklisp inclusion after the project builds reliably.

Until that work is done:

* Do not add `common-lisp` to `util/release-yamlscript`.
* Do not add a `common-lisp` case to `util/release-binding-published`.
* Do not publish a `yamlscript-common-lisp` repository tag as a substitute for
  an ecosystem package unless we explicitly choose that distribution model.

