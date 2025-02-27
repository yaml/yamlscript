---
title: YS Loader Libraries
talk: 0
---

YS is focused on providing a loader library for every programming language
where YAML is used.
Ideally it should be a drop-in replacement for the existing YAML loader library
you are using.

All existing YAML config files are already valid YS files and using the YS
loader to load them should work without any changes.
The advantage of using the YS loader is that they all work the same way and
provide the same capabilities, regardless of the underlying programming
language.

The following loader libraries are currently available:

* [Clojure](https://clojars.org/org.yamlscript/clj-yamlscript)
* [Go](https://github.com/yaml/yamlscript-go)
* [Java](https://clojars.org/org.yamlscript/yamlscript)
* [Julia](https://juliahub.com/ui/Packages/General/YAMLScript)
* [NodeJS](https://www.npmjs.com/package/@yaml/yamlscript)
* [Perl](https://metacpan.org/dist/YAMLScript/view/lib/YAMLScript.pod)
* [Python](https://pypi.org/project/yamlscript/)
* [Raku](https://raku.land/zef:ingy/YAMLScript)
* [Ruby](https://rubygems.org/search?query=yamlscript)
* [Rust](https://crates.io/crates/yamlscript)
