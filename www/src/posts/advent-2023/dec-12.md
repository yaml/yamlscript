---
title: Future Proof
date: '2023-12-12'
tags: [blog, advent-2023]
permalink: '{{ page.filePathStem }}/'
author:
  name: Ingy döt Net
  url: /about/#ingydotnet
---

Santa has very little margin for error.
He has to get everything just right all in one night.

YAMLScript is a work in progress, and will be for a long time.
I'm trying to get it right, but I'm no Santa!
In fact I'm quite sure I'll get some things wrong.
That's just the nature of the beast when you're a programmer.

Also, have you ever wondered why the magic YAMLScript starter tag has that `/v0`
at the end?

### Welcome to Day 12 of the YAMLScript Advent Calendar!

Today we'll discuss how YAMLScript is designed to be future proof.

I've been programming for quite a long time now.
I've learned that no matter how good I think an idea it today, I'll almost
certainly think of a better way to do it in the future.

This means I'll almost certainly want to make big changes to YAMLScript long
after it's in use in the real world.

I also believe that it is really important for things like APIs and progamming
languages to be stable and backwards compatible, basically forever.

If I could always have the freedom to make big changes to YAMLScript, but never
break backwards compatibility, then I could have my cake and eat it too.
And I think I can!

### The YAMLScript API Version

I call `v0` the YAMLScript API version.
YAMLScript v0 isn't done yet, but it should be in the next couple months.
At that point I'll release it as a stable version of the API version `0`.

So you've seen that to make a `.ys` file executable, you need to add a special
starter tag to the top of the file:

```yaml
--- !yamlscript/v0
say: "Hello!"
```

Or you can write it with the shebang line:

```yaml
#!/usr/bin/env ys-0
say: "Hello!"
```

Leaving off that `0` here will make things not work as you intended.

You might also have noticed that when you install the `ys` binary CLI is is a
symlink to `ys-0.1.26`.
And there is another symlink called `ys-0` that points to `ys-0.1.26`.

This is all by design.

The leading `0` in `!yamlscript/v0` and in `bin/ys-0` and also in `0.1.26` is
the YAMLScript API version.

When YAMLScript v0 is declared stable, there will never be changes that break
backwards compatibility with the `v0` API version.

That means you can write YAMLScript programs today, and they will continue to
work forever, even if you upgrade to a new version of YAMLScript.


### New Versions of YAMLScript

At some point after YAMLScript v0 is released as stable, I'll start working on
YAMLScript v1.

I can make any changes I want in YAMLScript v1, even ones that break backwards
compatibility with YAMLScript v0.
That's because I set up the rules such that you need to declare the version when
you write a YAMLScript program.


### Conclusion

This doesn't mean that I don't think extremely carefully about every change I
make to YAMLScript.
But it does mean that I don't have to worry about everything being perfect
before I release stable v0.

That's exciting as I strive to make YAMLScript a great language today and a
better language in the future.

I hope that also encorages you write more YAMLScript today, knowing that it will
continue to work in the future.

That wraps up Day 12.
And that also means we're half way through the Advent Calendar.
I hope you're enjoying it as much as I am, and I hope the best is yet to come!

Tune in tomorrow for Day 13 of the YAMLScript Advent Calendar.
