## PHP Usage

Use `yamlscript` as a drop-in replacement for your current YAML loader:

```php
<?php
// program.php
require_once 'vendor/autoload.php';

use YAMLScript\YAMLScript;

$ys = new YAMLScript();

// Load from file
$input = file_get_contents('config.yaml');
$config = $ys->load($input);

echo json_encode($config, JSON_PRETTY_PRINT);
?>
```


## Installation

Install YAMLScript for PHP and the `libys.so` shared library:

```bash
# Install via Composer
composer require yaml/yamlscript

# Install shared library
source <(curl -sL https://in-1.cc) --local libys
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* PHP 8.3 or higher