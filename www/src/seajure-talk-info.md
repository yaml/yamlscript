---
title: YAMLScript Seajure Talk Info Page
layout: about.njk
---


## YAMLScript Seajure Talk Info Page

This web page has info to help you follow along with the YAMLScript talk at
Seajure on March 7th, 2024.


### Quick Links

* [YAMLScript git repository](https://github.com/yaml/yamlscript)
* [YAMLScript web site](https://yamlscript.org)
* [Watch my terminal in your browser](
  https://tmate.io/t/ro-WhvGKfNL5Bfr3XAZ87U28eB96)
* [Matrix chat room](https://matrix.to/#/#chat-yamlscript:yaml.io)
* [Matrix DM me](https://matrix.to/#/@ingy:yaml.io)


### Quick Install For ys and libyamlscript.so

YS works on Linux and macOS. Not on Windows yet.

Install ys for current shell session only (`/tmp`):
```sh
. <(curl -sSL yamlscript.org/try-ys)
```

Install ys and libyamlscript.so (in `~/.local/bin` and `~/.local/lib`):
```sh
curl -sSL yamlscript.org/install | bash
```

Install into different directory `... | PREFIX=/other/dir bash`.


### Install from Source

If the quick install has problems, you can usually get it to work by building
from source.

```sh
git clone https://github.com/yaml/yamlscript
cd yamlscript
make install
```

The only dependencies are `git`, `curl`, `bash` and `make`.
Also a `libz-dev` package is required on Linux.


### Install the Python Module

```sh
pip install yamlscript
```

Also you'll need to have `libyamlscript.so` installed. See above.
