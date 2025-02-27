---
title: Compiling YS to Binary
talk: 0
---


You can compile any YS program that has a `main` function to a machine native
binary executable.

This is done using the `ys --compile --binary` command.

For example, if you have a file named `hello.ys` with the following content:

```yaml
!YS-v0

defn main(name='world'):
  say: 'Hello, $name!'
```

You can run:

```sh
$ time ys --compile --binary hello.ys
* Compiling YS 'hello.ys' to 'hello' executable
* Setting up build env in '/tmp/tmp.W0u4SGljdY'
* This may take a few minutes...
[1/8] Initializing              (3.1s @ 0.24GB)
[2/8] Performing analysis               (12.2s @ 0.64GB)
[3/8] Building universe         (1.8s @ 0.45GB)
[4/8] Parsing methods           (1.6s @ 0.46GB)
[5/8] Inlining methods          (1.4s @ 0.63GB)
[6/8] Compiling methods         (17.7s @ 0.43GB)
[7/8] Laying out methods                (1.0s @ 0.49GB)
[8/8] Creating image            (1.7s @ 0.54GB)
* Compiled YS 'hello.ys' to 'hello' executable

real    0m48.929s
user    6m44.965s
sys     0m5.095s
$ ls -lh hello
-rwxr-xr-x 1 me me 13M Sep 10 15:04 hello*
```

Note that the compilation takes some time and the resulting binary is quite
large.

Let's try it out:

```sh
$ time ./hello
Hello, world!

real    0m0.014s
user    0m0.002s
sys     0m0.013s
$ time ./hello Bob
Hello, Bob!

real    0m0.014s
user    0m0.005s
sys     0m0.010s
```

Let's compare the 14ms runtime to using the `ys` interpreter with `-e`:

```sh
$ time ys -e 'defn main(name="world"): say("Hello, $name!")'
Hello, world!

real    0m0.034s
user    0m0.019s
sys     0m0.017s
```

The binary is about 2.5 times faster than the interpreter in this case.

Note that we can even compile the one-liner if we want to:

```sh
$ ys --compile --binary -e 'defn main(name="world"): say("Hello, $name!")'
... time passes ...
* Compiled YS '-e' to './NO-NAME' executable
```

Since there is no input file, the binary is named `NO-NAME`.

Optionally, you can use the `--output=<file-name>` to specify the output file
name.

The options listed above have short names as well:

```sh
$ ys -cbo say-hi hello.ys
* Compiling YS 'hello.ys' to 'say-hi' executable
...
```
