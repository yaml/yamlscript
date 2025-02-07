---
title: Self Installation Scripts
---

<blockquote>

**Note**: If you just ran a program with `bash` that printed a URL
to this page, click the arrow below for more information on "What just
happened?".

<details><summary><strong>What just happened?</strong></summary>

If you are reading this you probably just ran a YS program with `bash`.
The first time you do that, the program installed the `ys` interpreter under the
`/tmp/` directory for you and then ran the program with it.
Subsequent runs of the program will use that installed `ys` interpreter.

You may continue to run the program this way, but there will be a slight delay
at the start each time while the `run-ys` auto-installer script is downloaded.

It is very easy to install the `ys` interpreter permanently on your system so
that you can run the program with `ys` instead of `bash`.

```bash
$ curl -s https://yamlscript.org/install-ys | bash
```

See the [YS Installation](install.md) page for more information.
</details>
</blockquote>

----

YS has a way to publish programs that people can run immediately without having
installed the `ys` interpreter first.

> **Warning**: See the [Security Considerations](
  #use-cases-and-security-considerations) below before using this technique.

Just begin your YS program with these lines:

```yaml
#!/usr/bin/env ys-0
source <(curl '-s' 'https://yamlscript.org/run-ys') "$@" :
```

Then anyone can run your program using Bash with a command like this:
`bash script.ys arg1 arg2 ...`.

The first time they do so, the `ys` interpreter will be downloaded and installed
under the `/tmp/` directory.

The `ys` interpreter will be downloaded only once, and it will be used for all
subsequent runs of the script.

> Note: The `curl` command will still download and evaluate the `run-ys` Bash
script on subsequent runs so the user will need to have internet access.

The program can also be run with the `ys` interpreter if the user installs it.
In that case the Bash installer line will be ignored.

Since the program has a shebang line, it can also be run as a `PATH` command
if the file is marked as executable.


## Example

Here's a small YS program that program that prints the ROT13 encoding of its
arguments:

```yaml
#!/usr/bin/env ys-0

source <(curl '-s' 'https://yamlscript.org/run-ys') "$@" :

alphabet =: set((\\A .. \\Z) + (\\a .. \\z))
rot13 =: cycle(alphabet).drop(13 * 2).zipmap(alphabet)

defn main(*input):
  say: str/escape(input.join(' ') rot13)
```

If we run it with `ys`:

```bash
$ ys rot13.ys I Love YS
V Ybir LF
```

If we run it with `bash`:

```bash
$ bash rot13.ys I Love YS
Installing the YS CLI '/tmp/yamlscript-run-ys/bin/ys-0.1.91' now...
Ctl-C to abort
See https://yamlscript.org/doc/run-ys for more information.

Installed /tmp/yamlscript-run-ys/bin/ys - version 0.1.91
--------------------------------------------------------------------------------
V Ybir LF
```

and again:

```bash
$ bash rot13.ys I Love YS
V Ybir LF
```


## How It Works

The program is both valid YS and valid Bash.

YS programs are required to start with a YAML tag like this:

```yaml
!yamlscript/v0
```

But if they start with a shebang line like this:

```yaml
#!/usr/bin/env ys-0
```

then the `!yamlscript/v0` tag is optional.

When you run the program with `bash`, the shebang line is merely a comment and
ignored by Bash.

The `source` line is a Bash command that reads and evaluates the contents of the
`<(...)` process substitution file.
The `curl` command inside downloads the `run-ys` script and installs the `ys`
interpreter under the `/tmp/` directory if it is not already installed.

It then `exec`s the installed `ys` interpreter with your original program and
any arguments you provided.

The `source` line is also a valid YS command.
It calls the YS `source` macro which ignores all of its arguments (much like
the `comment` macro does).

> Note: The `source` macro was added in YS version 0.1.85.
This technique will not work with earlier versions of YS.


## Use Cases and Security Considerations

This technique is may be useful in situations where you want to share a YS
program with people who are not yet familiar with YS.

Since the program is run with Bash which gets more Bash code from the internet,
it is subject to the many security risks of running arbitrary code from the
internet.

Caveat yamlscriptor!

> ** Note**: A more secure way to distribute a YS program is to
[compile it to a binary executable](binary.md) and distribute the binary
instead.

There is at least one use case where this Bash technique is safe and useful:

You can easily run a YS program that you are developing with a
particular version of the `ys` interpreter without having to install it first.
Just use the `YS_VERSION` environment variable to specify the version you want:

```bash
$ YS_VERSION=0.1.91 bash my-program.ys arg1 arg2 ...
```

This might be useful for testing a reported bug with an older version of the
interpreter, for example.

There may be other development and testing use cases for this technique as well.
If you find one, please let us know!
