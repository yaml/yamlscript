<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YS / YAMLScript
===============

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YS:
```yaml
!YS v0:

# Get data from external sources:
names-url =:
  "https://raw.githubusercontent.com/dominictarr/\
   random-name/master/first-names.json"

name-list =: &first-names json/load(curl(names-url))

# Data object with literal keys and generated values:
name:: rand-nth(*first-names)
aka:: name-list.rand-nth()
age:: &num 2 * 3 * 7
color:: &hue qw(red green blue yellow)
          .shuffle()
          .first()
title:: "$(*num) shades of $(*hue)."
```

and get:
```json
{
  "name": "Dolores",
  "aka": "Anita",
  "age": 42,
  "color": "green",
  "title": "42 shades of green."
}
```


## Description

[YS](https://yamlscript.org) is a functional programming language with a clean
YAML syntax.

YS can be used for enhancing ordinary [YAML](https://yaml.org) files with
functional operations, such as:

* Import (parts of) other YAML files to any node
* String interpolation including function calls
* Data transforms including ones defined by you

This YS library should be a drop-in replacement for your current YAML loader!

Most existing YAML files are already valid YS files.
This means that YS works as a normal YAML loader, but can also evaluate
functional expressions if asked to.

Under the hood, YS code compiles to the Clojure programming language.
This makes YS a complete functional programming language right out of the box.

Even though YS compiles to Clojure, and Clojure compiles to Java, there is no
dependency on Java or the JVM.
YS is compiled to a native shared library (`libyamlscript.so`) that can be used
by any programming language that can load shared libraries.

To see the Clojure code that YS compiles to, you can use the YS
CLI binary `ys` to run:

```text
$ ys --compile file.ys
(let
 [names-url "https://raw.githubusercontent.com/dominictarr/random-name/master/first-names.json"
  name-list (_& 'first-names (json/load (curl names-url)))]
 (%
  "name" (rand-nth (_** 'first-names))
  "aka" (rand-nth name-list)
  "age" (_& 'num (mul+ 2 3 7))
  "color" (_& 'hue (first (shuffle (qw red green blue yellow))))
  "title" (str (_** 'num) " shades of " (_** 'hue) ".")))
```


## NodeJS Usage

A YAMLScript file `some.ys`:

```yaml
!YS v0:

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

* [YS Web Site](https://yamlscript.org)
* [YS Blog](https://yamlscript.org/blog)
* [YS Source Code](https://github.com/yaml/yamlscript)
* [YS Samples](https://github.com/yaml/yamlscript/tree/main/sample)
* [YS Programs](https://rosettacode.org/wiki/Category:YAMLScript)
* [YAML](https://yaml.org)
* [Clojure](https://clojure.org)


## Authors


* [Ingy döt Net](https://github.com/ingydotnet)


## License & Copyright

Copyright 2022-2025 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
