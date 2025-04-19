---
title: YS by Example
talk: 0
---

One of the best ways to learn a new programming language is to see examples of
real code written in that language.
This page contains a links to programs written in YS.

!!! note

    The YS documentation is a work in progress.
    Looking at real life examples is a solid way to get started.
    If you feel the urge to contribute to the documentation, please do so.
    It would be greatly appreciated!

## YS Examples of Refactoring Large YAML Files

* [Calva's CircleCI Config](
  https://github.com/BetterThanTomorrow/calva/tree/published/.circleci#circleci-configyml)
* [HelmYS Helm Templating](
  https://github.com/kubeys/helmys?tab=readme-ov-file#helmys)


## YS Examples of Programs, Utilities and Automation

* [Rosetta Code](https://rosettacode.org/wiki/Category:YAMLScript)
  YS (YAMLScript) solutions to Rosetta Code tasks.
* [yamllm](https://github.com/yaml/yamllm/blob/main/bin/yamllm.ys)
  A command line multi LLM (Anthropic, OpenAI, Gemma) query tool.
* [sbs](https://github.com/ingydotnet/sbs/blob/main/bin/sbs)
  Creates markdown gists to compare pairs of files "side by side".
  Used to show differences between YS and Go templates in Helm.
  * [Example gist comparing Helm templates in YS and Go](
    https://gist.github.com/ingydotnet/ff0638edf1bcb53c45161dce2d777f74)
* [ys-vs-rc](
  https://github.com/ingydotnet/yamlscript-vs-rosetta/blob/main/bin/ys-vs-rc)
  Compares YS and Rosetta Code solutions to the same problem in many languages.
  * [Example gist comparing FizzBuzz in YS vs many other languages](
    https://gist.github.com/ingydotnet/9ece4af186c6a6dcfd589c446dab9b38)
* YS Repository Utilities
  * [util/release-yamlscript](
    https://github.com/yaml/yamlscript/blob/main/util/release-yamlscript)
    The utility that orchestrates the release of YS, including 12 binary builds
    and `libyamlscript.so` bindings for 11 programming languages.
  * [util/brew-update](
    https://github.com/yaml/yamlscript/blob/main/util/brew-update)
    The utility that updates the Homebrew formula for YS.
  * [util/mdys](
    https://github.com/yaml/yamlscript/blob/main/util/mdys)
    Renders Markdown with embedded YS code blocks.
    Used for the YS documentation.
  * [util/version-bump](
    https://github.com/yaml/yamlscript/blob/main/util/version-bump)
    Bumps the version of dozens of YS files in the repository at
    release time.


More examples will be added here as they become known.

If you have YS example code you'd like to share, please submit a PR to the
[YS Repo](https://github.com/yaml/yamlscript).
