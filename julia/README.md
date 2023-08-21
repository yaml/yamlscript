YAMLScript
==========

Program in YAML


## Status

The Julia implemention of YAMLScript is VERY alpha.

Please reach out if you are interested in contributing.

Try:
```
./bin/ys-yamlscript.jl test/hello.ys Julia
```


## Synopsis

A YAMLScript program `greet.ys` to greet someone 6 times:
```
#!/usr/bin/env yamlscript

defn main(name):
  say: "Hello, $name!"
```

Run:
```
$ yamlscript greet.ys YAMLScript
Hello, YAMLScript!
```


## Status

This is very ALPHA software.


## Authors

* Ingy döt Net <ingy@ingy.net>


## Copyright and License

Copyright 2022 by Ingy döt Net

This is free software, licensed under:

  The MIT (X11) License
