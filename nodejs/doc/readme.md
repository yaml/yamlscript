## NodeJS Usage

Use `@yaml/yamlscript` as a drop-in replacement for your current YAML loader:

```javascript
// program.js
const YS = require('@yaml/yamlscript');
const fs = require('fs');

const ys = new YS();

// Load from file
const input = fs.readFileSync('config.yaml', 'utf8');
const config = ys.load(input);

// Convert to JSON
console.log(JSON.stringify(config, null, 2));
```


## Installation

Install YAMLScript for Node.js and the `libys.so` shared library:

```bash
npm install @yaml/yamlscript
curl -sSL https://yamlscript.org/install | bash
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Node.js v18.0.0 or higher