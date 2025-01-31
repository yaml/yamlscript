---
title: Widget Testing
hide:
- navigation
- toc
---

<div class="grid" markdown>

=== "Unordered list"

    * Sed sagittis eleifend rutrum
    * Donec vitae suscipit est
    * Nulla tempor lobortis orci

=== "Ordered list"

    1. Sed sagittis eleifend rutrum
    2. Donec vitae suscipit est
    3. Nulla tempor lobortis orci

``` title="Content tabs"
=== "Unordered list"

    * Sed sagittis eleifend rutrum
    * Donec vitae suscipit est
    * Nulla tempor lobortis orci

=== "Ordered list"

    1. Sed sagittis eleifend rutrum
    2. Donec vitae suscipit est
    3. Nulla tempor lobortis orci
```

</div>

<div class="grid cards" markdown>
  - ### __YAML Made Better__
    ---
    :material-code-braces:{.lg .middle}
      [DevOps Perfected](doc/devops.md)  
    :material-code-braces:{.lg .middle}
      [YAMLScript in YAML](doc/examples.md)  
    :material-code-braces:{.lg .middle}
      [YAMLScript in Helm]()

  - ### __Full-featured YAML for All__
    ---
    :fontawesome-solid-truck-ramp-box:{.lg .middle}
      [YAML Loader libraries for all languages](doc/bindings.md)  
    :material-cog:{.lg .middle}
      [Fully configurable capabilities](doc/capabilities.md)  
    :material-security:{.lg .middle}
      [Secure and safe by design](doc/security.md)  
</div>

<div class="grid cards" markdown>
  - ### __Functional Programming in YAML__
    ---
    :fontawesome-solid-question:{.lg .middle}
      [Embeds cleanly in YAML](doc/ys.md)  
    :material-code-parentheses:{.lg .middle}
      [Import, Interpolate, Transform](doc/functions.md)  
    :simple-clojure:{.lg .middle}
      [100's of functions & Libraries](doc/functions.md)  
</div>

----

<div class="dark" markdown>

**YAMLScript**, or **YS** ("wise") is a functional programming
language with a clean syntax that is also 100% valid **[YAML](
https://yaml.org)**.
It was designed to be easily embedded into existing YAML files in order to
provide the logic, interpolation and data transformation capabilities that many
YAML users need.
Created by YAML inventor and lead maintainer, Ingy döt Net, YAMLScript solves
these needs beautifully for all YAML users and uses.


> YAMLScript is now available as a programming language learning track on
**[Exercism](https://exercism.org/tracks/yamlscript)**.
It's a great way to learn YAMLScript and get feedback from experienced mentors.
Check it out!

If you work with apps and frameworks that use YAML for configuration, you can
simplify your complex YAML files using YAMLScript, even if the app or framework
does not support it natively.
YAMLScript lets you include data from external files and other sources, make use
of hundreds of existing standard functions, and even define your own variables
and functions.
You can filter, map, merge, reduce, generate, concatenate, interpolate and
manipulate your data as you wish.
YAMLScript provides these things with syntax that is minimal and unobtrusive,
keeping your clean YAML data clean.

> Slides and information from Ingy's [KubeCon 2024 talk](
https://www.youtube.com/watch?v=Cdi3Q4Wrt48)
are available [here](https://yamlscript.org/kubeys24).
The highlight was [HelmYS](https://github.com/kubeys/helmys) a new Helm
post-renderer that lets you template Helm charts with YAMLScript (which is
actual YAML).

Like many new languages, YAMLScript was built over an existing robust language,
[Clojure](https://clojure.org), which in turn was built over
[Java](https://java.com).
The power of Clojure and Java is available to YAMLScript users via the
YAMLScript runtime interpreter, `ys`.
However, the `ys` command is compiled into a single standalone native binary
executable file.
This means that <u>No Java or JVM</u> installation is required to use
YAMLScript, and startup/execution speed is very fast.

YAMLScript also produces the `libyamlscript.so` shared library.
It has [binding modules for 10 programming languages](doc/bindings.md) including
Go, JavaScript, Python and Rust, with many more on the way.
These modules can be used in your programs to load normal YAML files as well as
YAMLScript enhanced ones.
</div>

There are many ways to use YAMLScript:

* Simplify your existing YAML configs
  * Works great with CI/CD, Helm, Docker, Ansible, etc.
  * Check out [HelmYS](https://github.com/kubeys/helmys), a tool for using
    YAMLScript in Kubernetes Helm chart templates painlessly.
* [Load YAMLScript (or YAML) in your programs](doc/bindings.md)
  * Available in 10 programming languages (and counting)
* Program in YAMLScript
  * Learn how at [Exercism](https://exercism.org/tracks/yamlscript)
* [Script Automation with YAMLScript](doc/examples.md)
  * Many used in the [YAMLScript repository](https://github.com/yaml/yamlscript)
* [Compile YAMLScript to binary executables](doc/binary.md)
  * Fast, standalone, no-source software distribution
* [Query and Transform YAML (and JSON) data](doc/query.md)
  * Use the `ys` command line tool similar to `jq` or `yq`


----

## YAMLScript Resources

* [Web Site](https://yamlscript.org)
* [Documentation](https://yamlscript.org/doc)
* [Matrix Chat](https://matrix.to/#/#chat-yamlscript:yaml.io)
* [Slack Chat](https://clojurians.slack.com/archives/yamlscript)
* [Blog](https://yamlscript.org/blog)
* [GitHub Repository](https://github.com/yaml/yamlscript)
* [Discussions](https://github.com/yaml/yamlscript/discussions)
* [Issues](https://github.com/yaml/yamlscript/issues)

----

## YAMLScript Links

* Nov 2024 [KubeCon Talk and Info](https://yamlscript.org/kubeys24)
* Jun 2024 [TPRC Talk](https://www.youtube.com/watch?v=RFIukRdFe1o)
* Apr 2024 [OSS/NA Talk](https://www.youtube.com/watch?v=u-OCEHNdwlU)
* Mar 2024 [TheNewStack Article](https://thenewstack.io/with-yamlscript-yaml-becomes-a-proper-programming-language/)
* Mar 2024 [Seajure Talk](https://www.youtube.com/watch?v=GajOBwBcFyA)

----
