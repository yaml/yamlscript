<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YAMLScript
==========

Program in YAML


## Synopsis

```yaml
#!/usr/bin/env ys-0

defn main(name='world'):
  say: "Hello, $name!"
```


## Description

YAMLScript is a functional programming language with a stylized YAML syntax.

YAMLScript can be used for:

* Enhancing ordinary YAML files with functional operations
  * Import parts of other YAML files to any node
  * String interpolation including function calls
  * Data transforms including ones defined by you
* Writing new programs and applications
  * Run with `ys file.ys`
  * Or compile to binary executable with `ys -C file.ys`
* Writing reusable shared libraries
  * High level code instead of C
  * Bindable to almost any programming language

YAMLScript should be a drop-in replacement for your YAML loader!

Most existing YAML files are already valid YAMLScript files.
This means that YAMLScript works as a normal YAML loader, but can also evaluate
functional expressions if asked to.

Under the hood, YAMLScript code compiles to the Clojure programming language.
This makes YAMLScript a complete functional programming language right out of
the box.

Even though YAMLScript compiles to Clojure, and Clojure compiles to Java, there
is no dependency on Java or the JVM.
YAMLScript is compiled to a native shared library (`libyamlscript.so`) that can
be used by any programming language that can load shared libraries.

To see the Clojure code that YAMLScript compiles to, you can use the YAMLScript
CLI binary, `ys`, to run:

```text
$ ys --compile file.ys
```


## NodeJS Usage

A YAMLScript file `some.ys`:

```yaml
!yamlscript/v0/

=>:
  name =: "World"
  data =: load("some.yaml")
  fruit =: data.food.fruit

num: 123
greet:: "$(data.hello.rand-nth()), $name!"
eat:: fruit.shuffle().first()
drink:: (["Bar"] * 3).join(', ' _).str('!!!')
```

A YAML file `some.yaml`:

```yaml
food:
  fruit:
  - apple
  - banana
  - cherry
  - date

hello:
- Aloha
- Bonjour
- Ciao
- Dzień dobry
```

NodeJS file `ys-load.js`:

```js
let fs = require("fs");
let YS = require("@yaml/yamlscript");

let input = fs.readFileSync("some.ys", "utf8");

let ys = new YS();

let data = ys.load(input);

console.log(data);
```

Run:

```text
$ node ys-load.js | jq
{
  num: 123,
  greet: 'Bonjour, World!',
  eat: 'cherry',
  drink: 'Bar, Bar, Bar!!!'
}
```


## Installation

You can install this module like any other NodeJS module:

```bash
$ npm install @yaml/yamlscript
```

but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```bash
$ curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/.local/bin` and `~/.local/lib` respectively.

See https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript for more info.


## See Also

* [The YAMLScript Web Site](https://yamlscript.org)
* [The YAMLScript Blog](https://yamlscript.org/blog)
* [The YAMLScript Source Code](https://github.com/yaml/yamlscript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors


* [Ingy döt Net](https://github.com/ingydotnet)


## License & Copyright

Copyright 2022-2024 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
