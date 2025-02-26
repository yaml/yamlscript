---
title: Coding with Style
# date: 2023-12-09
---

What's the best thing about Rudolph's nose?
Is it that lights the way for Santa's sleigh?
I'm calling BS on that.
I'd say it's the main thing that gives the whole **Sanata Story some Serious
Style!**

Good programmers do more than just get their solutions right.
They do the whole thing with style.
That makes the program easier to read, understand and maintain.
It also gives the code a certain je ne sais quoi.

### Welcome to Day 9 of the YS Advent Calendar

We know that YS compiles to Clojure.
And we know that YAML doesn't really look anything like a Lisp.
But it turns out that YS can use as much or as little Lisp style as you want it
to.
It's all about your personal style.

Here's a YS program that sings my favorite drinking song:

```yaml
#!/usr/bin/env ys-0
# 99-bottles.ys

# Print the verses to "99 Bottles of Beer"
#
# usage:
#   ys 99-bottles.ys [<count>]

defn main(number=99):
  each n (number .. 1):
    say: paragraph(n)

defn paragraph(num): |
  $bottles(num) of beer on the wall,
  $bottles(num) of beer.
  Take one down, pass it around.
  $bottles(num - 1) of beer on the wall.

defn bottles(n):
  cond:
    n == 0 : 'No more bottles'
    n == 1 : '1 bottle'
    else   : "$n bottles"
```

Let's give it a try:

```bash
$ ys 99-bottles.ys 3
3 bottles of beer on the wall,
3 bottles of beer.
Take one down, pass it around.
2 bottles of beer on the wall.

2 bottles of beer on the wall,
2 bottles of beer.
Take one down, pass it around.
1 bottle of beer on the wall.

1 bottle of beer on the wall,
1 bottle of beer.
Take one down, pass it around.
No more bottles of beer on the wall.
```

I feel tipsy.

Let's compile this program to Clojure and see what it looks like:

```clojure
$ ys -c 99-bottles.ys
(declare paragraph bottles)
(defn main
  ([number] (each [n (rng number 1)] (say (paragraph n))))
  ([] (main 99)))
(defn paragraph [num]
  (str (bottles num)
       " of beer on the wall,\n" (bottles num)
       " of beer.\nTake one down, pass it around.\n" (bottles (- num 1))
       " of beer on the wall." "\n"))
(defn bottles [n]
  (cond (= n 0) "No more bottles"
        (= n 1) "1 bottle"
        :else (str n " bottles")))
(apply main ARGS)
```

It turns out that the compiled Clojure code is actually valid YS syntax as well.
Well, almost.

The file is valid YAML.
It represents a single big string.
Normally YAML files represent a single big mapping or sequence, but they can
also define a top level scalar (string).

If I run this program though, it doesn't print anything.
The reason is simple.
We didn't give it the power to.
We didn't add the `!YS-v0` tag to the beginning of the file.

If we do that it works fine.

So apparently we can write YS in a purely Lisp style.
The truth is you can write YS is a completely YAML style (no parens), a
completely Lisp style (all parens), or somewhere in between.

Good looking YS programs start with YAML style and then switch to the Lisp style
for certain nicer idioms.
Note that you can't switch back to YAML style once you've switched to Lisp
style.

Let's iterate on the above code and make it YAML just at the top level:

```yaml
defn main:
  ([number] (each [n (rng number 1)] (say (paragraph n))))
  ([] (main 99))
defn paragraph [num]:
  (str (bottles num)
       " of beer on the wall,\n" (bottles num)
       " of beer.\nTake one down, pass it around.\n" (bottles (- num 1))
       " of beer on the wall." "\n")
defn bottles [n]:
  (cond (= n 0) "No more bottles"
        (= n 1) "1 bottle"
        :else (str n " bottles"))
apply: main ARGS
```

That already looks a lot better.

We get a big win turning the `paragraph` into a YAML literal scalar (heredoc)
with string interpolation.

```yaml
defn paragraph(num): |
  $bottles(num) of beer on the wall,
  $bottles(num) of beer.
  Take one down, pass it around.
  $bottles(num - 1) of beer on the wall.
```

That's so much easier to read and understand.

I hope you are inspired to write some beautiful YS code.

**You've got style, baby!**

Come back tomorrow for day 10 of the YS Advent Calendar.
