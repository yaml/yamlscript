---
title: YS and CircleCI
---

Here is an example of refactoring a CircleCI `config.yml` file to YS:
https://github.com/BetterThanTomorrow/calva/blob/published/.circleci/README.md

What started out as one big [636 line YAML file](
https://github.com/ingydotnet/calva/blob/9483828c23d117cf8f3c5cfe2ec2bef7be3b51f6/.circleci/config.yml)
with lots of embedded Bash scripts, was refactored into [nearly 40 small files](
https://github.com/BetterThanTomorrow/calva/tree/published/.circleci) each with
a clear and specific purpose.

You can see the how the migration was made over 30 single purpose commits in
[this Pull Request](
https://github.com/BetterThanTomorrow/calva/pull/2511).

Now changes are made in a much more maintainable way, and the CircleCI required
`config.yml` file is generated from the YS files using a simple `ys` command:

```
$ ys -Y config.ys > config.yml
```

As a bonus, linting was performed on the Bash scripts (now in their own files)
and several small issues were found and fixed.

----

This technique is a general strategy for refactoring large YAML files into
smaller, more maintainable ones, regardless of whether or not the target system
uses YS natively yet.
