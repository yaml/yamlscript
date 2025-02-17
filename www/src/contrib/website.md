---
title: Website Contribution
---


## Please and Thank You

The YS website needs your help!
Especially with the docs.

Everyone is welcome to contribute to the website, and your contributions are
greatly appreciated.


## How to Contribute

The yamlscript.org website is designed to be very easy to contribute to.

Every single page on the site is created from a Markdown file in the
[YS repository](https://github.com/yaml/yamlscript).
This repo is a mono-repo, meaning that all parts of the YS project is in there.

Also, every page in the site (except the front page) has a "view source" button
and an "edit this page" button.
These buttons will take you to the source file for that page.

THe "edit" button will drop you into the GitHub editor for that page.
You can make your changes and then submit a pull request.

This is the easiest way to contribute to the site for small changes.


## Bigger Changes

If you want to add new pages of content, or make more significant changes, you
should clone the repo and work on it locally.
When you are ready to submit your changes, you can create a pull request.

Testing your changes locally is incredibly easy.
To get started, run thesee commands:

```bash
$ git clone https://github.com/yaml/yamlscript
$ cd yamlscript
$ git checkout website  # PRs should be made against the "website" branch
$ cd www
$ make serve
```

The `make serve` command will install all the dependencies in the `./.venv/` and
in `/tmp/yamlscript/` and then start a local webserver at
<http://localhost:8000>.

It will aslo start a watcher that will automatically rebuild the site when you
make changes to the source files.
These  changes will be visible in your browser immediately.


## Important Source Files

The [YS website](https://yamlscript.org) is built using the
[Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) statis site
framework, which is quite incredible in terms of what it can do (and easily).

!!! note

    You don't need to know anything about MkDocs to make changes to the site,
    but if you want to add plugins or make theme customizations, you will need
    to learn a bit.

The prerequisites for building the site are very minimal:

* Linux or MacOS
* Python 3.6+
* Python `pip`
* GNU `make`
* `curl`
* `bash`

!!! note

    The `bash` command just needs to be in your `PATH`; it doesn't need to be
    your interactive shell.

The most important files for the website are:

* `www/Makefile` - This `Makefile` is used to handle all the various
  orchestration tasks you may need to do.
* `www/config.ys` - The main configuration file for the site. Written in YS.
* `www/config/` - The YAML config files for various aspects of the site.
* `www/src/` - The source files for the site.
* `doc/` - The source files for the documentation.
* `blog/` - The source files for the blog.
* `requirements.txt` - The Python requirements for the site.
* `theme/**` - The theme override files for the site.


## Ask Us for Help

If you need help with anything, start a discussion [here](
https://github.com/yaml/yamlscript/discussions).
