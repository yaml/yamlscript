## Ruby Usage

File `prog.rb`:

```ruby
require 'yamlscript'
input = IO.read('file.ys')
ys = YAMLScript.new
data = ys.load(input)
puts data
```

File `file.ys`:

```yaml
!YS-v0:

name =: "World"

foo: [1, 2, ! inc(41)]
bar:: load("other.yaml")
baz:: "Hello, $name!"
```

File `other.yaml`:

```yaml
oh: Hello
```

Run:

```text
$ ruby prog.rb
{"foo"=>[1, 2, 42], "bar"=>{"oh"=>"Hello"}, "baz"=>"Hello, World!"}
```


## Installation

You can install this module like any other Ruby module:

```bash
gem install yamlscript
```

but you will need to have a system install of `libyamlscript.so`.

One simple way to do that is with:

```bash
curl https://yamlscript.org/install | bash
```

> Note: The above command will install the latest version of the YAMLScript
command line utility, `ys`, and the shared library, `libyamlscript.so`, into
`~/local/bin` and `~/.local/lib` respectively.

See <https://yamlscript.org/doc/install/> for more info.
