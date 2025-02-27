<!-- DO NOT EDIT — THIS FILE WAS GENERATED -->

YS / YAMLScript
===============

Add Logic to Your YAML Files


## Synopsis

Load `file.yaml` with YS:
```yaml
!YS-v0:

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

## Go Usage

In `go.mod`:

```go
require github.com/yaml/yamlscript-go v0.1.93
```

File `prog.go`:

```go
package main
package main

import (
        "fmt"
        "github.com/yaml/yamlscript-go"
)

func main() {
        data, err := yamlscript.Load("a: [b, c]")
        if err != nil {
                return
        }
        fmt.Println(data)
}
```



## Installation

You can install this module like any other Go module:

```bash
$ go get github.com/yaml/yamlscript-go@v0.1.93
```

but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```bash
$ curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/local/bin` and `~/.local/lib` respectively.

See https://github.com/yaml/yamlscript?#installing-yamlscript for more info.


### Environment Variables

At the current time, you will need to set 3 environment variables to use the
module:

```bash
export CGO_CFLAGS="-I $HOME/.local/include"
export CGO_LDFLAGS="-L $HOME/.local/lib"
export LD_LIBRARY_PATH="$HOME/.local/lib"
```


## Go User Feedback

This is a very early version of yamlscript-go.
Your feedback is very welcome.
Please open an issue on this repository or chat with us directly at
<https://matrix.to/#/#chat-yamlscript:yaml.io>.

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
* [Andrew Pam](https://github.com/xanni)

## License & Copyright

Copyright 2022-2025 Ingy döt Net <ingy@ingy.net>

This project is licensed under the terms of the `MIT` license.
See [LICENSE](https://github.com/yaml/yamlscript/blob/main/License) for
more details.
