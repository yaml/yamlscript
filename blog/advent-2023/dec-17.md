---
title: Rosetta Code
# date: 2023-12-17
---

How does Santa read all the signs in all the languages of the world?
That's a lot of languages to know on top of all the other things he has to do.
Luckily he has his trusty polyglot elf, Rosetta, at his side.
Bet you didn't know that!


### Welcome to Day 17 of the YS Advent Calendar

A cool guy named Mike Mol made a website called [Rosetta Code](
https://rosettacode.org/wiki/Rosetta_Code) for programmers who want to
learn all the programming languages of the world!

It's a wiki that has well over 1000 programming tasks and almost 1000
programming languages.
The idea is for people to contribute solutions to the tasks in as many
languages as they can.

I've been a fan of Rosetta Code for years.
Here's one of my all time favorite programs from the site:
[FizzBuzz in SNUSP](
https://rosettacode.org/wiki/FizzBuzz/EsoLang#SNUSP)!!

One thing people like to use Rosetta Code for is to show off a new language.
YS is a new language, and I've put a few solutions up there myself:

* [99 Bottles of Beer](
  https://rosettacode.org/wiki/99_Bottles_of_Beer#YAMLScript)
* [FizzBuzz](
  https://rosettacode.org/wiki/FizzBuzz#YAMLScript)
* [Hello world/Text](
  https://rosettacode.org/wiki/Hello_world/Text#YAMLScript)
* [Factorial](
  https://rosettacode.org/wiki/Factorial#YAMLScript)
* [Fibonacci sequence](
  https://rosettacode.org/wiki/Fibonacci_sequence#YAMLScript)

Adding solutions to Rosetta Code is a one of the best ways I've found to figure
out what YS needs to do and what it needs to do better.


### Rosetta Code Data

The Rosetta Code website is a great resource, but it's a bit clunky to use.
It would be really cool if all the code examples were available in a nice
Git repository.

Well, it turns out that they are!
You can clone over 100,000 code examples in a few seconds by running this
command:

```bash
$ git clone https://github.com/acmeism/RosettaCodeData
$ cd RosettaCodeData
$ ls -l Lang/YAMLScript/
total 8
00-LANG.txt
00-META.yaml
99-bottles-of-beer -> ../../Task/99-bottles-of-beer/YAMLScript/
Factorial -> ../../Task/Factorial/YAMLScript/
Fibonacci-sequence -> ../../Task/Fibonacci-sequence/YAMLScript/
FizzBuzz -> ../../Task/FizzBuzz/YAMLScript/
Hello-world-Text -> ../../Task/Hello-world-Text/YAMLScript/
```

The Rosetta Code Data Project is something I wrote almost 15 years ago.
This last summer I was able to make it a lot easier to update.

Let's try out the FizzBuzz example:

```bash
$ ys Task/FizzBuzz/YAMLScript/fizzbuzz.ys 16
1
2
Fizz
4
Buzz
Fizz
7
8
Fizz
Buzz
11
Fizz
13
14
FizzBuzz
16
```

Pretty cool, huh?


### RC Needs More YS!

Let's try adding a new YS solution to Rosetta Code.

The first thing to do is to find a task that doesn't have a YS solution yet.
You can find all the tasks listed [here](
https://rosettacode.org/wiki/Category:Programming_Tasks).

Let's take the first one: [100 doors](
https://rosettacode.org/wiki/100_doors).

What I like to do next is cheat!
Clojure has solved most of the tasks on Rosetta Code, and YS is another way to
write Clojure, so let's just pick a Clojure solution and translate it to YS.

```clojure
(defn doors []
	(reduce (fn [doors idx] (assoc doors idx true))
	        (into [] (repeat 100 false))
	        (map #(dec (* %1 %1)) (range 1 11))))

(defn open-doors [] (for [[d n] (map vector (doors) (iterate inc 1)) :when d] n))

(defn print-open-doors []
  (println
    "Open doors after 100 passes:"
    (apply str (interpose ", " (open-doors)))))

(print-open-doors)
```

Here's a quick translation to YS:

```yaml
!yamlscript/v0

defn doors():
  reduce:
    fn(doors idx): assoc(doors idx true)
    into []: repeat(100 false)
    map \(dec (%1 * %1)): 1 .. 10

defn open-doors():
  for [d n] map(vector doors() iterate(inc 1)) :when d: n

defn print-open-doors():
  say:
    "Open doors after 100 passes:
    $(apply str interpose(\", \" open-doors()))"

=>: print-open-doors()
```

Let's see if it works:

```bash
$ ys 100-doors.ys
Open doors after 100 passes: 1, 4, 9, 16, 25, 36, 49, 64, 81, 100
```

Great! Now let's clean it up a bit:

I'm just going to rename the `print-open-doors` function to `main` and move it
to the top of the file.
The `main` function (if defined) is called automatically when the script is run.

```yaml
!yamlscript/v0

defn main():
  say:
    "Open doors after 100 passes:
    $(apply str interpose(\", \" open-doors()))"

defn open-doors():
  for [d n] map(vector doors() iterate(inc 1)) :when d: n

defn doors():
  reduce:
    fn(doors idx): assoc(doors idx true)
    into []: repeat(100 false)
    map \(dec (%1 * %1)): 1 .. 10
```

I also ordered the functions top-down in the order they are called since we know
that YS will auto-declare them in the generated Clojure code.

```bash
$ time ys 100-doors.ys
Open doors after 100 passes: 1, 4, 9, 16, 25, 36, 49, 64, 81, 100

real    0m0.041s
```

Still works!
Pretty fast too!
Let's add it to Rosetta Code!

To do this you need to create an account on the site, login, and then click the
"Edit" tab on the task page.
It's a pretty terrible plain text editor, but scroll way down to where
the YS solutions should go and add this:

```text
=={&lcub;header|YAMLScript}}==
&lt;syntaxhighlight lang="yaml">
!yamlscript/v0

defn main():
  say:
    "Open doors after 100 passes:
    $(apply str interpose(\", \" open-doors()))"

defn open-doors():
  for [d n] map(vector doors() iterate(inc 1)) :when d: n

defn doors():
  reduce:
    fn(doors idx): assoc(doors idx true)
    into []: repeat(100 false)
    map \(dec (%1 * %1)): 1 .. 10
&lt;/syntaxhighlight>
{&lcub;out}}
&lt;pre>
$ ys 100-doors.ys
Open doors after 100 passes: 1, 4, 9, 16, 25, 36, 49, 64, 81, 100
&lt;/pre>
```

Click "Save changes" and you're done!

* <https://rosettacode.org/wiki/100_doors#YAMLScript>
* <https://github.com/acmeism/RosettaCodeData/blob/main/Task/100-doors/YAMLScript/100-doors.ys>
  * I updated the repo (not automatic yet)

**We did it!**

----

Don't wait for me to add more YS solutions to Rosetta Code.
You can do it yourself!
If you do, send me (`@ingydotnet`) a DM on the [Rosetta Code Discord](
https://discord.com/channels/1011262808001880065/) server and I'll update the
Rosetta Code Data Project repo with your solutions.

If you had never heard of Rosetta Code before, I hope you'll find it as
interesting as I do.

Check back tomorrow for Day 18 of the YS Advent Calendar!
