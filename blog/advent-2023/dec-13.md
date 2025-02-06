---
title: History Lesson
# date: '2023-12-13'
# tags: [blog, advent-2023]
# permalink: '{{ page.filePathStem }}/'
# author:
#   name: Ingy döt Net
#   url: /about/#ingydotnet
---

Santa is Legend.
Legends have histories.
The histories of Santa are many and varied, some going back to the 4th century
AD.

YAMLScript's history is much shorter, but it's still a history.
Today I'd like to tell you a little bit about it.


### Welcome to Day 13 of the YAMLScript Advent Calendar!

In the summer of 2022 I was gathering information about a computer system that I
was learning about.
I was putting the information into a YAML file.
At first I thought to myself, this YAML file could essentially be a
configuration file for running the system.
Then I thought, what if I could run the system by just "running" the YAML file?

Hmmmm... YAMLScript!
For me a project can only be started when it has a name.
I had the name, I loved the name, I loved the idea, I started the project!

I put together a prototype as a Perl module and published it to CPAN.
I knew that conceptually YAMLScript would be a Lisp, but to be honest I barely
knew what a Lisp was.


### One True YAML

Earlier in the year I had come to the conclusion that the YAML spec and all of
the YAML implementations were too hard to fix.
I had spent the second half of 2021 trying to fix YAML, with a [core group](
https://yaml.org/spec/1.2.2/ext/team/) of five people, and it slowly became
obvious that it was a lost cause.
It was just too much work to get everyone to agree on anything, let alone
everything.

A possibly better solution was to create a new YAML framework (based on the
current YAML 1.2 spec) and publish it in 42 different languages.

At least this way there would be a set of quality YAML implementations that were
the same in every language.
The main problem with this idea was simply learning (and remembering!) how to
publish a library in 42 different languages!

I decided to write a meta publishing framework that would allow me to publish
to any language using the same simple commands.
I called this framework [PST - Package Super Tool](
https://github.com/ingydotnet/pst).

In doing that I had to pick 42 languages and start learning a bit about each of
them.
At some point I came across Clojure and thought it was interesting.


### Make a Lisp

Around the start of 2023 I started thinking about YAMLScript again.
My fledgling language with just a toy implementation in Perl.
I knew YAMLScript was going to be a Lisp, but I didn't know much about Lisps.
I asked the internet how to make a Lisp and it told me to read [Make a Lisp](
https://github.com/kanaka/mal/blob/master/process/guide.md)!

Make a Lisp (Mal) is a project that walks you through making a baby
Clojure-inspired Lisp implementation in the programming language of your choice.
It has eleven chapters, each with a set of tests that you must pass before you
can move on to the next chapter.
At the end you have a working Lisp implementation and you have to use it to run
the Mal implementation written in Mal and make sure it passes all the tests!

I decided to do Mal in Perl (even though there was already a Perl
implementation).
There are nearly 100 implementations of Mal in over 70 different languages.
It took me about 2 weeks to get through the whole thing.
By the end I really felt like I knew what a Lisp was, how Lisps worked and how
to make one.

When I finish the Mal course I decided to do it again but this time in
YAMLScript, a language that didn't exist yet.
This was really easy and fun.
I just ported the Mal in Mal code to a YAML form that I found pleasing and
acceptable as a programming language; essentially making up YAMLScript as I went
along.

I also wanted to go much further with the Perl implementation.
I decided I would create a full implementation of Clojure on Perl based off of
my Mal in Perl code.
I call this project [Lingy](https://github.com/lingy-lang/lingy) (currently
[available on CPAN](https://metacpan.org/dist/Lingy)).
This is when I started learning Clojure. (Spring 2023).


### TPRC 2023

I was also helping to organize the [TPRC 2023](https://tprc.to) (Perl and Raku)
conference.
I decided I would give a talk called [Lingy and YAMLScript](
https://www.youtube.com/watch?v=9OcFh-HaCyI).
I had to get a working Lingy and a working YAMLScript implementation in Perl by
the end of June.

After I gave the talk I noticed it was getting a lot of attention.
A lot more views on youtube than any other talk at the conference.
I realized this was happening because the words "Clojure" and "Java" were in the
talk description.


### The Clojure Community

A lovely man named Peter Strömberg reached out to me and asked if I would be
interested in joining the [Clojure Community Slack](
https://clojurians.slack.com/).
Peter is the author of [Calva](https://calva.io/), a Clojure IDE for VS Code,
one of the best ways to write and work with Clojure.
I joined right away and he pointed me in all the right directions and got me
talking to all the right people.

I soon learned about [GraalVM](https://www.graalvm.org/) and [SCI](
https://github.com/babashka/sci) written by Michiel Borkent, a man to whom I owe
much credit for helping me with many deep technical issues I encountered.
Michiel is the author of [Babashka](https://babashka.org/), a popular way to do
shell scripting in Clojure.
SCI is the Clojure runtime engine that Babashka uses and that YAMLScript now
uses as well!

All the pieces started falling into place, and I got a clear vision of what
YAMLScript should become... AND how relatively easy it would be to make it
happen thanks to the Clojure ecosystem.
I started rewriting the YAMLScript compiler and runtime in Clojure and
rebuilding it into what it is today.


### December 1st, 2023

At some point in the Fall, I decided that I would start blogging about
YAMLScript every day in December 2023, in the Programming Project Advent
Calendar style.

I thought I would easily get everything done by Dec 1st but to be honest, even
as I write this now, there is still so much to do.
Some days I really want to write about a particular topic, but the code isn't
quite ready yet.

But all that said, it's all working out pretty dang well.
I'm loving this language and project!

I have the highest hopes of hopes for YAMLScript in 2024.

There's a lot more details I could add here but it's getting late and I need to
get some sleep.


### Join me again tomorrow for Day 14 of the YAMLScript Advent Calendar!
