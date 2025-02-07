---
title: Naughty is Nice!
# date: 2023-12-15
---

As the architect of a major world holiday, Santa Claus has hard design choices
to make.
What is Suki going to get this year?
He keeps it simple with the standard Naughty-Or-Nice algorithm.

As architects of an aspiring new programming language, the YS folks have design
choices to make as well!

Naughty-Or-Nice should not be discounted but what about Naughty-And-Nice?
Naughty-Xor-Nice???

My personal favorite?

**Naughty-_Is_-Nice!!**


### Welcome to Day 15 of the YS Advent 2023!

Today I want to carry on a bit about what I think about whilst designing YS
syntax, semantics and features.

The unwavering rule here is that YS needs to be written as valid YAML, and it
needs to become valid Clojure.

Those two things don't look much alike.
However, remember that JSON is a subset of YAML and JSON does look a little bit
more like a Lisp (Clojure is a Lisp).
YAML's data model is a superset of JSON's data model.
So there are pathways and connections to be made between YS and Clojure.

Given that, my next move could have been to make YS be as close to Clojure as
possible (while still satifying those constraints).
I did try that for about 7 minutes...
The results were just not "feel good".

Time for Plan B.


### mal.ys

I mentioned this in my recent [History Lesson](dec-13.md)...
I had just finished my Perl implementation of [Make a Lisp](
https://github.com/kanaka/mal) (mal) and I decided to try to make a YS version
next even though there was no YS yet.

Take a look at the implementation of [Mal in Mal](
https://github.com/kanaka/mal/blob/master/impls/mal/stepA_mal.mal).

As I said before when you complete the exercise of writing a version of Mal in
the programming language of your choice, your final goal is to pass the Mal test
suite by running the Mal implementation of Mal using your newly created Mal
implementation.

I'll admit that even now, that is a bit mind bending!

I decided to port the Mal in Mal implementation (a Lisp) to YS.
I revised it over and over until I liked how it looked and felt.
Certainly I had to think about how my new creation would scale to other
YS programming problems.

Here's what I came up with on March 12th, 2023: [yamlscript.ys](
https://github.com/ingydotnet/mal/blob/yamlscript/impls/yamlscript/src/yamlscript.ys).

This code hurts my eyes now.
YS has come a long way since then.

The point here is that my modus operandi remained the same.
Write programs in YS (often by porting them from existing Clojure programs) and
make sure they don't suck.
Keep refining the language until it feels right from all angles.


### Breaking the Rules

Clojure and Lisps in general have very simple syntax rules.
Nested parens with a function name first, followed by arguments.
When things don't feel right in certain cases, Lisps have their coveted macros
to rearrange the things in parens to look nicer and work as intended.

Lispers take pride in this simplicity and uniformity.
They tend to look down on their bespoken cousins like Perl, Python and Ruby;
even though those languages give much credit to Lisp for their dynamic natures.

I'm new to Lisp, but as the author of a new programming language written
entirely in a Lisp, I can honestly say that I enjoy it.

That said, I embrace diversity and I enjoy many different styles of programming.
I'm also not afraid to break a few rules when it makes sense.

I guess if there's one technology that I know and am most known for, it's YAML.
I'd like to think that I know what YAML users like and what things they desire
that YAML doesn't provide.

I should say "didn't provide", because YS is going to provide almost anything
they could want.

If I were to list the top 3 things that YAML users want:

* Composability via file inclusion
* String interpolation (aka templating)
* Simple composition and transformation functions:
  * Map merging
  * Sequence concatenation
  * String manipulation
  * etc.

Technologies that have made major use of YAML, like Kubernetes, Ansible and
OpenAPI, have all had to invent their own ways to provide these features.

With YS, all the things they've done and more are available everywhere and work
the same for everyone.
And all without breaking the things those technologies already do.

I suppose this is all to say that in general YS will follow Clojure's lead when
its awesome, but will also look for cleaner ways to do things when it feels
needed.

Maybe you think I'm being Naughty.
I am, but… **Naughty is Nice!**


### The Naughty List (aka The Nice List :- )

say what you want…

No, literally…

`say` it!

In Clojure you use `println` to print a string with a newline added to the end.
In Python it's `print`.
Ruby `puts` it out there.
Perl people `say` things.

I've always been a fan of doing more with less.
Clojure code overall makes it easy to accomplish big things with less code.
But from the word by word perspective, Clojure is a bit long-winded at times.

YS has a `say` function in the standard library that does the same thing as
`println` in Clojure (but with 4 less letters).
But I didn't take `println` away from you either.
That's your choice.

YS is about choices.
It embraces the [TMTOWTDI](
https://en.wikipedia.org/wiki/There%27s_more_than_one_way_to_do_it) philosophy.
You saw yesterday how many different ways there are to call functions in YS.
I tend to respect function names whose length is inversely proportional to how
often they are used.

Another good example is Clojure's `*command-line-args*` dynamic variable.
YS calls it `ARGV`, a shorter name that is seen in other languages.

----

I'm here, Doc!

One thing that YAML has that Clojure doesn't is "heredocs".
A heredoc is a way to write a multiline string without having to escape any of
the characters in it.
That means you can write almost any string in your program to look exactly like
it would in a text file or printed out.

Ruby has heredocs because it stole them from Perl who in turn stole them from
Shell.
YAML has literal scalars which are even better than heredocs because they have
no ending marker.
You can embed any text content simply by indenting it appropriately.
YAML has this awesome feature because I loved it in Perl and wanted it in YAML!
True story.

YS extends YAML's literal scalars by adding interpolation to them.
Using the `$some-var` or `$(some-expression)` inline syntax, YS will eval and
insert the values just like you'd expect.

This makes things like templating and testing multi-line output a breeze.
In fact, the YS test suite (testing YS inputs, Clojure outputs, and
intermediary stage forms) is based on [YAML Literals](
https://github.com/yaml/yamlscript/blob/main/core/test/compiler-stack.yaml).

----

YeS, we can!

We talked about the YeS Expression Syntax of YS a few days ago.
One thing Lisps can't do is have real infix math expressions like:
`(1 + 2 * 3)`.
Macros can reorder things but the macro name has to be first.
Thus you could do `(infix 1 + 2 * 3)` but at that point you've already lost.

YS tries to support as much infix expressions as it can while still keeping
things sane, readable and predictable.
You can write `1 + 2` to mean `(+ 1 2)` and `1 + 2 + 3` to mean `(+ 1 2 3)`.
But you can't write `1 + 2 * 3` because that involves precedence, and that's
where things get messy.

In a day or two I'll show you YS's path lookup syntax which looks like:
```yaml
val =: obj.foo.3."bar baz".map(inc)
```

This a is very readable, obvious and powerful way to chain lookups and function
calls together.

But it's really just using the same infix syntax as above with the YS `.`
chaining operator!

----

How do you usually structure your programs?

I'm a top-down kind of guy.
I like to start with the main entry point function calling other functions and
then define those functions (which use other functions to be defined later).

Clojure is a bottom-up language by nature.
You can't call a function until it's defined.
Well, you can, but you have to use the `declare` function to do it.
The Clojure people I've interacted so far tend to avoid `declare` and just go
bottom-up.

YS wants you to be you!

You can write things in any order you want to.

A big advantage of compiling to an existing language is that you are free to
generate uglier output code (that is rarely seen) to support prettier input code
(that you have to look at all the time).

In this case, the YS compiler scans the AST near the end of the compilation and
finds all the functions that were called before they were defined.
It then inserts a `declare` expression for them at the top of the output code.

Problem solved!

!!! note

    If you are interested in why Clojure's author Rich Hickey chose to make
    Clojure "bottom-up", read about it [here](
    https://gist.github.com/reborg/dc8b0c96c397a56668905e2767fd697f#why-clojure-compiler-is-single-pass-arent-many-possible-optimizations-lost-this-way).

    Then read the rest of that page.
    It's full of great insights into the design of Clojure and programming in
    general.

----

When I started designing the YAML language in 2001 the main goals were:

* Make something awesome (that my Mom could use)
* More content, less markup
* See/share data the same in any programming language

The primary audience for YS is people who already use YAML and wish it could do
more.
Clojure and Lisp have so much to offer other programming languages.
It's like a deep well of awesome that YS users can tap into when they are
ready.
But if they just want to `load` and `merge` some YAML files, like they saw their
friends doing, they can stop there.

I hope that gives you a taste of the kinds of things I think about when I'm
designing YS.
You can't please everyone, but if you give people choices, the only people that
get upset are the ones who don't like choices. :- )

See you tomorrow? That's Day 16 of the YS Advent 2023!!!
