---
title: About This Website
talk: 0
---

This website was built using the (_**OMG it's the best site generator in the
universe!!!**_) [Material for MkDocs](
https://squidfunk.github.io/mkdocs-material/) theme for the [MkDocs](
https://www.mkdocs.org/) static site generator, and many of its plugins.


## YS Enhancement

While the Material for MkDocs theme is already feature-rich, we improved things
a bit more by using YS.

We also use a [Makefile](
https://github.com/yaml/yamlscript/blob/main/www/Makefile) to automate all the
MkDocs build steps like installing Python packages, creating and enabling a
virtual environment, building the site, and serving it both locally and on
GitHub Pages.

All you need to do in the [YS mono-repo's `www` directory](
https://github.com/yaml/yamlscript/blob/main/www/) is run these `make` commands:

* `make serve` — Serve the site locally (after doing any necessary setup)
* `make publish website=stage` — Publish the site to
   <https://stage.yamlscript.org>
* `make publish website=live` — Publish the site to
   <https://www.yamlscript.org>
* `make clean` — Remove the generated files
* `make realclean` — Remove the generated files and the virtual environment

So simple!


### Generating the `mkdocs.yml`

MkDocs uses a `mkdocs.yml` YAML file for its configuration.
As that file grew in size, we found it easier to manage by splitting it into
multiple files, and using the `config.ys` file to include them.

* [`config.ys`](https://github.com/yaml/yamlscript/blob/main/www/config.ys)
  — The main configuration file
* [`config/*.yaml`](https://github.com/yaml/yamlscript/blob/main/www/config/)
  — The included configuration files


### MarkdownYS

Certain pages (like library method documentation) are annoying to write because
they require a lot of repeated boilerplate.
We made a tool called **`mdys`** that lets you write YAML data inside of
Markdown and specify a YS function to turn it into Markdown.

If you have the documentation for 100 methods and later you want to change the
formatting for them, you only need to tweak the YS function.

A good example of this is the [YS Standard Library documentation](
doc/ys-std.md).
Click on the "Edit this page" link to see the source.

The YAML data starts with:

````
```mdys:stdlib
String functions:
- base64-decode(Str) Str: Decode a base64 string
````

This YAML gets loaded into an object and passed to the [`mdys-stdlib`](
https://github.com/yaml/yamlscript/blob/website/doc/mdys.ys#L7-L17)

!!! note

    The `Makefile` takes care of watching any `.mdys` files and converting them
    to `.md` files whenever they change (when serving the site locally).

    Working with MarkdownYS in MkDocs is as easy as working with Markdown.
