# Prolog Binding Under Construction

The YAMLScript Prolog binding is currently experimental and not wired into the
release process.

The implementation in this directory targets Trealla Prolog.
It uses Trealla-specific FFI predicates such as `'$dlopen'` and
`'$register_function'` to load the YAMLScript native library.

Trealla is actively maintained, but it does not appear to have a public package
registry or package publishing workflow comparable to registries used by other
YAMLScript bindings.
For that reason, the current binding should not be treated as a publishable
Prolog package yet.

The likely publishing path is to support SWI-Prolog and publish through the
SWI-Prolog pack system.
That would require porting the native binding to SWI's foreign language
interface and validating that the pack can be installed and tested by SWI's
package tooling.

Until that work is done:

* Do not add `prolog` to `util/release-yamlscript`.
* Do not add a `prolog` case to `util/release-binding-published`.
* Do not publish a `yamlscript-prolog` repository tag as a substitute for an
  ecosystem package unless we explicitly choose that distribution model.

