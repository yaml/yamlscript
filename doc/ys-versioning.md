---
title: YS Versioning
comments: true
---


YS is a "versioned" programming language.

It uses the [Semantic Versioning](https://semver.org/) scheme.

Major compatibility breaking changes are allowed between major versions.

All programs and data files that use YS code expressions must declare the major
version of YS they use.

The current major version is `0` and that means that the language is still
under development.
That said, the language has been fairly stable for a while now.

Very soon we will declare v0 stable and start working on v1.
Soon after that v1 will be declared stable and we'll start working on v2.
The v1 "development" period is just a practice run for switching major versions.
v1 stable should be nearly identical to v0 stable.

In fact, we plan to deprecate v0 after v1 is stable.
After a couple months of v0 deprecation, use of v0 will be disallowed.

The v2 "development" period will be a much longer period.

The stable v2 runtime will support both v1 and v2 programs and libraries.

See [YS v1 Planning](v1-planning.md) for a description of the changes we need to
make to release YS v0 as a stable YS v1, and then start working on YS v2.


## Rules for version usage

YS programs must declare their major version.

They usually do this with a stream tag like `!YS/v1`.

They can also use a shebang line to declare the version.

```yaml
#!/usr/bin/env ys-1
```

You need the `-1` part or `!YS/v1` won't be added.
You could, of course, do this:

```yaml
#!/usr/bin/env ys
!YS/v1
```

!!! note "-e versioning"

    When using the `-e` flag, the expression is assumed to be code mode and an
    explicit `!YS/v1` tag is not needed.
    This is for simplicity of 1-liners.

    The version is determined by the major version of the `ys` binary being
    used.


## Rules for new version development

Work on v2 might last a year.
During that time, we will be putting out new v1 releases.

We will also be putting out new v2 alpha releases on a regular basis.

We want to let people start using v2 as soon as possible.

This requires that we have a v2 versioning scheme in place, and that we have
syntax rules to use v2.

These rules should be compatible with semver rules.


### Current v0 binary naming

* `<prefix>/bin/ys` (symlink)
* `<prefix>/bin/ys-0` (symlink)
* `<prefix>/bin/ys-0.1.97`
* `<prefix>/lib/libyamlscript.so-0.1.97`


### New v1 naming

* `<prefix>/bin/ys` (symlink)
* `<prefix>/bin/ys1` (symlink)
* `<prefix>/bin/ys-v1` (symlink)
* `<prefix>/bin/ys-1.2.3`
* `<prefix>/lib/libyamlscript.so-1`

When v1 is final stable, we just need to match the major version.


### New v2 naming

For the time where v1 is final stable and v2 is in development.

During development the version will always start with `2.0.0`.
Then we'll have a `-alpha` suffix followed by a minor and patch version.

When alpha-minor is zero then any change can happen.
This is like a normal major version of `0`.

So we start at `2.0.0-alpha.0.1`.
This is kinda like a `2.0.1` version that is alpha (mentally removing the
`.0.0-alpha.` part).
When we go to `2.0.0-alpha.1.1` we've made come to a point where things are
stabilizing and we want to indicate a breaking change by bumping the alpha-minor
number.

This scheme is compatible with semver and ever increasing versions.


### New v2 binary naming

* `<prefix>/bin/ys` (symlink)
* `<prefix>/bin/ys2a` (symlink)
* `<prefix>/bin/ys-2a` (symlink)
* `<prefix>/bin/ys-2.0.0-alpha.0.1`
* `<prefix>/lib/libyamlscript.so-2.0.0-alpha.0.1`


### v2 shebangs

To use a shebang for v2 while it is still in development, we can:

```yaml
#!/usr/bin/env ys-2a
```
