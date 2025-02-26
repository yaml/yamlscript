---
title: YS and GitHub Actions
---

[RapidYAML](https://rapidyaml.readthedocs.io/latest/) is the world's fastest and
most correct YAML parser written in C++.

It uses [GitHub Actions (GHA)](https://github.com/features/actions) workflows to
build and test the codebase for a variety of platforms and configurations.

The YAML files for these workflows are have been [converted to YS](
https://github.com/biojppm/rapidyaml/tree/master/.github/workflows-in)
resulting in a much more concise, maintainable and readable set of workflows
files (that do exactly the same thing as before).

GitHub Actions is a really well thought out system for using YAML as a language
to define CI/CD workflows.
That said, YS can be used to make these workflows cleaner and more enjoyable to
work with.


!!! note "RapidYAML coming to YS Soon"

    The YS and RapidYAML authors have been working together to bring the power of
    RapidYAML to the YS compiler.
    Currently the YS compiler uses [SnakeYAML Engine](
    https://bitbucket.org/asomov/snakeyaml-engine) (which is quite good) for its
    parsing stage.
    We hope that by offering RapidYAML as an option, we can make YS even faster and
    more correct.
