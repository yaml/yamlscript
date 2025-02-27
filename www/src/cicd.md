---
title: YS for CI/CD
talk: 0
---

YS is a great language for CI/CD pipelines.

Regardless of what CI/CD system you are using, you can use YS to make those
configuration files much more maintainable and powerful.


## The Problem

CI/CD pipelines are often written in YAML but YAML is not a programming
language.
This means that you can't do things like:

* Load data from other files, databases, APIs, URLs, etc
* Query data structures to get the data you need
* Access environment variables
* Assign variables
* Reference other parts of the YAML file
* Interpolate variables and function calls into strings
* Use loops and conditional logic
* Transform data structures using 100s of built-in functions
* Define and call your own functions
* Use external libraries
* Run shell commands


## The Solution

YS can do all of these things, and more, and it does it all in a way that is
completely compatible with YAML.
You can start with your existing YAML files and add YS capabilities to them as
much or as little as you need.


## The Next Problem

YS is a new language, and your CI/CD system probably doesn't support it out of
the box.

The YS team hopes that over time YS YAML loaders will become the preferred way
to load YAML config file in almost any situation, including CI/CD systems.


## The General Solution

Until then, you can use YS to generate the YAML files that your CI/CD system
expects.

A typical way to do this is to copy your existing YAML file, say
`pipeline.yaml`, and then run this command:

```bash
ys -Y pipeline.ys > pipeline.yaml
```

This will generate a new `pipeline.yaml` file that should be semantically
equivalent to the original `pipeline.yaml` file.
The comments and formatting will be different, but the data should be the same.

Next you'll want to add that command to your CI/CD system so that it runs every
time you make a change to your `pipeline.ys` file.
This could be as simple as adding a Makefile (see below) with a Git hook, but
that's up to you.


### First time validation

After converting your `pipeline.yaml` file to `pipeline.ys` the first time, you
should validate that the conversion is correct.

One way to do this is by running:

```bash
$ ys -pe 'ARGS.0:yaml/load-file == ARGS.1:yaml/load-file' -- \
    <(git show HEAD:pipeline.yaml) pipeline.yaml
true
```

If the output is `true`, then the conversion is correct.
If the output is `false`, then you'll need to investigate why.

After the first time, you can refactor your `pipeline.ys` file with YS any way
you like.
The resulting `pipeline.yaml` file should never change at all.
This makes refactoring so much easier, because you will know immediately if you
broke something. (if the `pipeline.yaml` file is modified)


### Using a Makefile

When you use YS to generate your YAML files, you'll only be editing the YS files
but you need to make sure that the YAML files are always up to date.

There are many ways to do this, but one simple way is to use a Makefile.

Here is an example Makefile that will generate any files in your project that
end in `.ys` from the corresponding `.yaml` file:

```makefile
SHELL := bash

YS_VERSION := 0.1.93
YS := /tmp/bin/ys-$(YS_VERSION)
YS_FILES := $(shell find . -name '*.ys')
YAML_FILES := $(YS_FILES:.ys=.yaml)

update: $(YAML_FILES)

test: update
        @git diff --quiet --exit-code -- $(YAML_FILES) || { \
          git diff -- $(YAML_FILES); \
          echo "ERROR: YAML files are out of date"; \
          echo "Run 'make update' to update them"; \
          exit 1; \
          }
        @echo "PASS: All YAML files are up to date"

%.yaml: %.ys $(YS) Makefile
        @echo "# GENERATED FILE. EDIT '$<' INSTEAD." > $@.tmp
        $(YS) -Y $< >> $@.tmp
        mv $@.tmp $@

$(YS):
        curl -s https://getys.org/ys | \
          PREFIX=/tmp VERSION=$(YS_VERSION) QUIET=1 bash \
          > /dev/null

.PHONY: update test
```

Every time you change a `.ys` file, you can run `make test` to make sure that
the corresponding `.yaml` file is up to date.
You'll see a diff of the changes if there are any.
If you are refactoring your `.ys` file, you can run `make update` to make sure
nothing has changed in the `.yaml` files.

!!! note

    Notice how the Makefile adds a comment to remind you that the `.yaml` files
    are generated and should not be edited directly.

    This Makefile even downloads the `ys` binary for you, so you don't need to
    have it pre-installed on your system.
    In fact it installs a specific version of `ys` so you can be assured that
    the generated YAML files are always the same.


### Using a Git hook

You can add a Git pre-push hook to make sure that the `.yaml` files are up to
date before you push your changes.

If the updated `.yaml` files have changed, the commit will be aborted and you'll
need to commit the changes before you can push.
