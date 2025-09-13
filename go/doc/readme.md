## Go Usage

Use `yamlscript-go` as a drop-in replacement for your current YAML loader:

```go
// program.go
package main

import (
    "fmt"
    "io/ioutil"
    "log"

    "github.com/yaml/yamlscript-go"
)

func main() {
    // Load from file
    content, err := ioutil.ReadFile("config.yaml")
    if err != nil {
        log.Fatal(err)
    }

    data, err := yamlscript.Load(string(content))
    if err != nil {
        log.Fatal(err)
    }

    fmt.Printf("%+v\n", data)
}
```


## Installation

Install the Go module and the `libys.so` shared library:

```bash
go get github.com/yaml/yamlscript-go@latest
curl -sSL https://yamlscript.org/install | bash
```

Set required environment variables:

```bash
export CGO_CFLAGS="-I $HOME/.local/include"
export CGO_LDFLAGS="-L $HOME/.local/lib"
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Go 1.18 or higher
* CGO enabled