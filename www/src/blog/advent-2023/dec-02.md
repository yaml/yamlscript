---
title: Twas a Bit
date: '2023-12-02'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---


> 'Twas a bit before Hanukkah, and all through the igloo,  
not a creature was stirring, not even a frog.  
The stockings were hung by the window with care,  
In hopes that St. Krampus soon would be there.  
The offspring were nestled all snug in their bunks,  
While visions of spicy-cookies danced in their heads.  
And cuz in their 'kerchief, and I in my bonnet,  
Had just settled down for a long winter's snooze.  
When out on the lawn there arose such a clatter,  
I sprang from my bench to see what was the matter.  
Away to the window, I flew like a jet,  
Tore open the shutters and threw up the curtain.  
The asteroid on the breast of the new-fallen frost,  
Gave the lustre of mid-day to dirt below.  
When what to my wondering eyes should materialize?  
But a miniature car, and eight tiny elephants.

Well that was a bit weird.
Let's try again.
Don't worry, it's not that hard to write Winter holiday poetry when you have
YAMLScript on your side!


### Welcome to day 2 of YAMLScript Advent 2023!

Today we're going to write a program that generates a winter holiday poem in the
Mad Libs style.
Along the way, we'll learn about several of YAMLScript's basic language
features, including:

* The `load` function
* Defining functions
* Calling functions
* Variables
* Random numbers
* String interpolation

So where is this awesome YAMLScript poetry generator?
Well, it's right here, of course; wrapped up in a nice little package for you to
open and enjoy!

<details><summary><strong style="color:green">Open Me!!!</strong></summary>

```yaml
{% include "../../../main/sample/advent/twas-a-bit" %}
```
</details>

You can run this program with one of the following commands:

```bash
$ ys twas-a-bit

$ chmod +x twas-a-bit && ./twas-a-bit
```

and you'll (very likely) get a different version of the poem every time you run
it!

You might be wondering where all the data for this poem comes from.
I hid it in a secret place... see if you can find it!

<details><summary><strong style="color:red">Don't Open Me!!!</strong></summary>

```yaml
{% include "../../../main/sample/advent/a-bit-of-data.ys" %}
```
</details>


### The `load` Function

If you found the secret database you can see that it's just a YAML file.
Our YAMLScript program loads this file using the `load` function.
The `load` function is one of the most important functions in YAMLScript.
It reads a YAMLScript file, evaluates it, and returns the result.

But we loaded a YAML file, not a YAMLScript file.
How did that work?
Remember that (almost) every YAML file is a valid YAMLScript program, so it just
works.

We can also use it to load a JSON data file.
Why?
Because JSON is a subset of YAML.
That means that (absolutely) every JSON file is a valid YAML file, and therefore
every JSON file is a valid YAMLScript program!

The `load` function is just one of literally hundreds of core functions
available to you by default in YAMLScript.
It's part of the YAMLScript standard library.
These functions give you the ability to code anything possible in any modern
programming language.
We'll be learning more about them in the coming days.


### Defining Functions

We defined 3 functions in our poem generator: `main`, `W`, and `poem`.
We did this using `defn`, which is short for "define function".

Here's a simple function that tells you how far away you are from the answer to
life, the universe, and everything:

```yaml
defn how-far-away(n): abs(42 - n)
```

In YAML terms this is a mapping pair whose key is a plain (unquoted) string
containing `defn`, the function name, and the function parameters.
The mapping pair's value is the function body, which is a YAMLScript expression.

A YAMLScript function returns the value of its last evaluated expression.


### Calling Functions

There are a lot of ways to call a function in YAMLScript.
In fact, there are a lot of ways to do almost everything in YAMLScript.
The only hard and fast rule is that the entire program must be valid YAML.

Consider this expression:

```yaml
map inc: range(1, 10)
```

Here we are calling 2 functions: `map` and `range`.
Another way to write this would be:

```yaml
map inc:
  range: 1, 10
```

or:

```yaml
=>: map(inc, range(1, 10))
```

or:

```yaml
->>: range(1, 10), map(inc)
```

There's actually many more ways to write this, but this is only day 2 of
YAMLScript Advent 2023, so we'll save those for later.

In general, these 2 forms are equivalent:

```yaml
a b, c: d, e, f
---
a(b, c, d, e, f)
```

In YAMLScript, a function call is either a mapping pair or a scalar.
When it is a mapping pair, the key and the value can both have 1 or more
expressions.
The first expression on the key side is the function name and all the rest of
the expressions on both sides are the function arguments.
When it is a scalar, a function name is followed by a parenthesized list of
arguments.
Note that the opening parenthesis must immediately follow the function name
without any intervening whitespace.

You may have noticed a few more functions just now: `inc`, `=>`, and `->>`.
Well, `inc` is a function but we didn't call it directly.
We passed it as an argument to the `map` function.

I'll tell you about `=>` and `->>` later.


> ### Comma Chameleon
>
> I was going to save this for later, but I just can't wait...
>
> **Commas are a Lie!**
>
> In YAMLScript, commas are optional whitespace.
> You can use them to make your code more readable, if that's your thing.
> It's not my thing, so I won't use them much from here on out.


### Variables

One of my favorite things about YAMLScript is that you can use any word as a
variable or function name.
We call these names "symbols".

For example almost every language has an `if` keyword, and (since it is a
special keyword) you can't use it for a name.
In YAMLScript you are free to use the `if` symbol (as a variable or function
name) as long as you don't need to use the standard `if` function in the same
scope.

We assign a value to a variable like so:

```yaml
foo =: 42
```

This is a mapping pair whose key is a plain string containing the variable name
followed by `=:` separated by at least one space character.
The pair value is the assignment value.
Simple!


### Strings

If you know your YAML, you know that YAML has 5 different styles to represent
a scalar value (a string, number, boolean, etc).
The styles are called "plain" (unquoted), "single-quoted", "double-quoted",
"literal", and "folded".
Plain scalar turns strings, numbers, booleans, etc.
The other 4 styles are always strings.

In YAMLScript code, the scalar style is very important.
Symbols and expressions are always plain scalars.
The 4 other styles are used for strings.


### Random Numbers

We used the `rand-nth` function to select a random value from a list.
Not much to say about that, except that it's a function that takes a list as an
argument and returns a random value from it.


### String Interpolation

YAMLScript strings written in the "single-quoted" or "folded" styles are just
strings, but when you use the "double-quoted" or "literal" styles, the strings
have interpolation super powers!

Interpolation is the process of inserting the value of variables or expressions
into a string in the place where they appear.
In YAMLScript, these expanding objects are indicated by a `$` character
immediately followed by the variable or expression.

This is pretty much the same as string interpolation in a shell like Bash:

```bash
name=World
echo "Hello $name. The answer is $((43 - 1))."
```

The equivalent YAMLScript code would be:

```yaml
name =: 'World'
say: "Hello $name. The answer is $(43 - 1)."
```

You can also interpolate a YAMLScript function call (symbol followed by argument
list) like this:

```yaml
say: "Hello $inc(41)."
```

In most languages that support interpolation `inc` would expand as a variable,
but in YAMLScript it's a function and the parenthesized argument list is part of
the expression.

This is the kind of interpolation syntax we used in our poem generator program.


### Conclusion

Well that wraps up day 2 of the YAMLScript Advent 2023.
I hope you enjoyed it, and learned a bit more about YAMLScript.

Honestly, at this point you probaby have more questions than answers.
Luckily for you, December has more than 2 days in it.
See you tomorrow!

I'll leave you with a little poem that a close friend of mine just wrote:

> 'Twas a bit before Winter, and all through the hut,  
not a creature was stirring, not even a snake.  
The stockings were hung by the faucet with care,  
In hopes that St. Frosty soon would be there.  
The tots were nestled all snug in their bunks,  
While visions of pungent-candies danced in their heads.  
And uncle in their 'kerchief, and I in my fedora,  
Had just settled down for a long winter's rest.  
When out on the lawn there arose such a clatter,  
I sprang from my sofa to see what was the matter.  
Away to the window, I flew like a bullet,  
Tore open the panels and threw up the blind.  
The planet on the breast of the new-fallen snow,  
Gave the lustre of mid-day to flowers below.  
When what to my wondering eyes should materialize?  
But a miniature train, and eight tiny oxen.
