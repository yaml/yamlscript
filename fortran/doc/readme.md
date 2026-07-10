## Fortran Usage

The `load` function compiles and evaluates a YAMLScript (or plain
YAML) string and returns the raw JSON response string from `libys`:
`{"data": ...}` on success or `{"error": {"cause": ...}}` on failure.
Fortran has no standard JSON library, so parsing the response is left
to the caller (for example with the
[json-fortran](https://github.com/jacobwilliams/json-fortran)
package).

File `main.f90`:

```fortran
program main
  use yamlscript
  implicit none

  type(yamlscript_t) :: ys
  character(len=:), allocatable :: json

  call ys%init()
  json = ys%load('!ys-0:' // new_line('a') // 'key:: inc(41)')
  print *, json
  call ys%destroy()
end program main
```


## Installation

Add `yamlscript` to your `fpm.toml` and install the `libys.so` shared
library:

```toml
[dependencies.yamlscript]
git = "https://github.com/yaml/yamlscript-fortran"
tag = "v0.2.27"
```

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

Build with the libys library path:

```bash
fpm build --flag "-L$HOME/.local/lib"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* GFortran and the Fortran Package Manager (fpm)
* Linux or macOS
