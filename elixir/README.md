YAMLScript
==========

Program in YAML — Code is Data


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

* [Ingy döt Net](https://github.com/ingydotnet)


## Copyright and License

Copyright 2022-2024 by Ingy döt Net

This is free software, licensed under:

  The MIT (X11) License
