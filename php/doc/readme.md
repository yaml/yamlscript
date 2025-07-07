# YAMLScript PHP

The PHP implementation of YAMLScript.

## Installation

```bash
composer require yaml/yamlscript
```

## Usage

```php
use YAMLScript\YAMLScript;

$ys = new YAMLScript();
$result = $ys->load("inc: 41");
echo $result;  // Outputs: 42
```

## Testing

```bash
composer test
```

## Requirements

* PHP 8.3 or later
* Composer
* Required PHP extensions:
  * dom
  * json
  * libxml
  * mbstring
  * tokenizer
  * xml
  * xmlwriter
