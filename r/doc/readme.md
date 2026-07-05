## R Usage

Use `yamlscript` as a drop-in replacement for your current YAML
loader:

File `main.R`:

```r
library(yamlscript)

input <- paste(readLines("config.yaml"), collapse = "\n")
data <- yamlscript_load(input)
str(data)
```


## Installation

Install the `yamlscript` R package from GitHub and the `libys.so`
shared library:

```r
remotes::install_github("yaml/yamlscript-r")
```

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* R 4.0 or higher (with a C compiler for the package build)
* The jsonlite package
* Linux or macOS
