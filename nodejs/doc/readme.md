## NodeJS Usage

A YAMLScript file `some.ys`:

```yaml
!YS-v0:

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

but you will need to have a system install of `libys.so`.

One simple way to do that is with:

```bash
$ curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libys.so`, into
`~/.local/bin` and `~/.local/lib` respectively.

See <https://yamlscript.org/doc/install/> for more info.
