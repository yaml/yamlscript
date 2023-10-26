---
title: Perl to Rust
date: '2023-12-23'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

When Santa is doing his job in the Luxembourg area, I've always wondered how he
gets from Perl to Rust.

Maybe he takes [this route](
https://oylenshpeegul.gitlab.io/from-perl-to-rust/introduction.html)!


### Welcome to Day 23 of the YAMLScript Advent Blog!

A couple of days ago we showed you how to use YAMLScript from Python.

One language binding down, forty-one to go!

Today we'll show you how to use YAMLScript with 3 new programming language
bindings: **Perl**, **Rust** and **Raku** (aka Perl 6).

YAMLScript gets by with a little help from its friends:

* [@tony-o](https://github.com/tony-o) - Raku binding
* [@ethiraric](https://github.com/ethiraric) - Rust binding
* [@jjatria](https://github.com/jjatria) - Perl binding
* [@vendethiel](https://github.com/vendethiel) - Polyglot extraordinaire

These guys are awesome!


### I Heard a Rumor!

Let's make up a little YAMLScript program that we can run from all the new
YAMLScript bindings:

```yaml
# hearsay.ys

{% include "../../../main/sample/advent/hearsay.ys" %}
```

Now run (actually "load") this a few times using the YAMLScript `ys --load`
command:

```bash
$ ys --load hearsay.ys | jq -r .
I heard that @tony-o uses YAMLScript in their Rust code!
$ ys --load hearsay.ys | jq -r .
I heard that @ethiraric uses YAMLScript in their Python code!
$ ys --load hearsay.ys | jq -r .
I heard that @jjatria uses YAMLScript in their Rust code!
$ ys --load hearsay.ys | jq -r .
I heard that @ingydotnet uses YAMLScript in their Perl code!
$ ys --load hearsay.ys | jq -r .
I heard that @vendethiel uses YAMLScript in their Raku code!
$
```

Works like a charm!

**Now let's load this program from each of the new language bindings!**


### Perl

I've been programming Perl for a very long time.
25 years actually.
I've published over 200 Perl modules on CPAN.
My first one was called [Inline::C](
https://metacpan.org/pod/Inline::C) which makes it trivial to write C bindings
in Perl.

That's exactly what I needed to get done today to write this blog post about it.
Ironically, I've forgotten how to use Inline::C, so I asked an AI to do it for
me.
It gave me something reasonable looking, but I couldn't get it working.

But the best part of Perl is its community!
My good Perl friend [Olaf](https://metacpan.org/author/OALDERS) told me to seek
out a Perl programmer named [JJ](https://metacpan.org/author/JJATRIA).
I did and he was happy to help.
He got it done in no time, and now I'm writing about it!!!

JJ used the newer Perl FFI binding framework called [FFI::Platypus](
https://metacpan.org/pod/FFI::Platypus).

Let's use the new CPAN module YAMLScript to run our `hearsay.ys` program:

```perl
# hearsay.pl
{% include "../../../main/sample/advent/hearsay.pl" %}
```

```bash
$ perl hearsay.pl
I heard that @ethiraric uses YAMLScript in their Rust code!
$ perl hearsay.pl
I heard that @ingydotnet uses YAMLScript in their Python code!
```

Just like the Python binding, the Perl module has a `load` method that takes a
YAMLScript program as a string and returns the result as a Perl data structure.

Install YAMLScript with:

```bash
$ cpanm YAMLScript
```

Also like the Python binding, the Perl module (and all the other bindings)
currently requires that you install `libyamlscript.so` yourself.

You can do this easily with:

```bash
$ curl -s https://yamlscript.org/install-libyamlscript | bash
```

Remember that this installs to `/usr/local/lib` by default, so you'll need to
run this as root.
(Or use the PREFIX option and set `LD_LIBRARY_PATH` yourself.)


### Rust

@ethiraric is a Rust programmer who dropped by the [YAML matrix chat](
https://matrix.to/#/#chat:yaml.io) a couple of weeks ago looking to improve
Rust's YAML support.
I told him about YAMLScript and suggested he write a Rust binding for it since
it's just one FFI call.

He did and today we get to show it off.

Rust needs a bit more setup than Perl, but it's still pretty easy.

First run `cargo new hearsay` to create a new Rust project.
Then edit `hearsay/Cargo.toml` to look like this:

```toml
[package]
name = "hearsay"
version = "0.1.0"
edition = "2021"
[dependencies]
yamlscript = "0.1.2"
```

Then edit `hearsay/src/main.rs` to look like this:

```rust
fn main() {
    let input = std::fs::read_to_string("hearsay.ys").unwrap();
    let output = yamlscript::load(&input).unwrap();
    println!("{output}");
}
```

Now run `cargo run` and you should see something like this:

```bash
$ cargo run
    Finished dev [unoptimized + debuginfo] target(s) in 0.01s
     Running `target/debug/hearsay`
{"data":"I heard that @ethiraric uses YAMLScript in their Python code!"}
```

That's not quite right, but that's where things are at this moment.
The `load` function is returning a JSON string, but it should be returning a
Rust data structure for whatever was under the `"data"` JSON key.
I'm sure @ethiraric will fix it soon!!!

It's actually fortunate that the Rust binding is not working yet, because it
shows us _how_ libyamlscript actually works.

The libyamlscript library currently has a single function that takes a
YAMLScript string and returns a JSON string.
Internally it compiles the YAMLScript to Clojure and evaluates the code using
SCI.
Then it converts the result to JSON and returns it.
If the evaluation fails it returns JSON with all the error information under an
`"error"` key.
If successful, it returns JSON with all the result information under a `"data"`
key.

The above call was successful, so that's why we see our expected result under
the `"data"` key.



### Raku

Raku is the new name for Perl 6.
It's a completely different language than Perl 5, but it's still a lot like
Perl.

@tony-o is a Raku programmer and my personal friend IRL for many years now.
He really loves YAMLScript and wants to work on the language as a whole.
Writing a Raku binding was a perfect way to get him started.

You can install the Raku binding with:

```bash
$ zef install YAMLScript
```

Here's the example Raku program:

```perl
{% include "../../../main/sample/advent/hearsay.raku" %}
```

Then you can run our hearsay program like this:

```bash
$ LD_LIBRARY_PATH=/usr/local/lib raku hearsay.raku
I heard that @tony-o uses YAMLScript in their Python code!
```

The Raku effort was a two person job.
A big shout out to @vendethiel for helping @tony-o get the Raku binding working
right.

Ven, as we call him, is someone I've known of and highly respected for many
years.
He was a major contributor to both CoffeeScript and Raku which have permanent
places in my heart.
He's Polyglot to the core and possibly more [Acmeist](https://acmeism.org) than
Ingy!

But my biggest thanks to Ven is for being my daily sounding board and
protaganist for YAMLScript.
He encourages my good ideas even when they are ambitious and crazy sounding.
Every time he's disagreed with me, he's been right...
Even if it sometimes takes me a while to see it.

Everyone in this penuiltimate advent blog post is a hero to me and definitely on
Santa's nice list!!! (as far as I'm concerned)

Please do join me tomorrow for the final post of the YAMLScript Advent Blog
2023!

{% include "../../santa-secrets.md" %}
