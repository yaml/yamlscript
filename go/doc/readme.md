## Go Usage

In `go.mod`:

```go
require github.com/yaml/yamlscript-go v0.1.89
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
$ go get github.com/yaml/yamlscript-go@v0.1.89
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
