## Dart Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

File `main.dart`:

```dart
import 'dart:io';

import 'package:yamlscript/yamlscript.dart';

void main() {
  final ys = YAMLScript();
  final input = File('config.yaml').readAsStringSync();
  final data = ys.load(input);
  print(data);
  ys.dispose();
}
```


## Installation

Add the `yamlscript` package to your project and install the `libys.so`
shared library:

```bash
dart pub add yamlscript
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Dart SDK 3.0 or higher
* Linux, macOS or Windows
