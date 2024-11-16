---
title: YAMLScript by Example
---

One of the best ways to learn a new programming language is to see examples of
real code written in that language.
This page contains a links to programs written in YAMLScript.

> **Note:** YAMLScript's documentation is a work in progress.
> Looking at real life examples is a solid way to get started.
> If you feel the urge to contribute to the documentation, please do so.
> It would be greatly appreciated!

## YAMLScript Examples of Refactoring Large YAML Files

* [Calva's CircleCI Config](
  https://github.com/BetterThanTomorrow/calva/tree/published/.circleci#circleci-configyml)
* [HelmYS Helm Templating](
  https://github.com/kubeys/helmys?tab=readme-ov-file#helmys)


## YAMLScript Examples of Programs, Utilities and Automation

* [Rosetta Code](https://rosettacode.org/wiki/Category:YAMLScript)
  YAMLScript solutions to Rosetta Code tasks.
* [yamllm](https://github.com/yaml/yamllm/blob/main/bin/yamllm.ys)
  A command line multi LLM (Anthropic, OpenAI, Gemma) query tool.
* [sbs](https://github.com/ingydotnet/sbs/blob/main/bin/sbs)
  Creates markdown gists to compare pairs of files "side by side".
  Used to show differences between YS and Go templates in Helm.
  * [Example gist comparing Helm templates in YAMLScript and Go](
    https://gist.github.com/ingydotnet/ff0638edf1bcb53c45161dce2d777f74)
* [ys-vs-rc](
  https://github.com/ingydotnet/yamlscript-vs-rosetta/blob/main/bin/ys-vs-rc)
  Compares YAMLScript and Rosetta Code solutions to the same problem in many
  languages.
  * [Example gist comparing FizzBuzz in YAMLScript vs many other languages](
    https://gist.github.com/ingydotnet/9ece4af186c6a6dcfd589c446dab9b38)
* YAMLScript Repository Utilities
  * [util/release-yamlscript](
    https://github.com/yaml/yamlscript/blob/main/util/release-yamlscript)
    The utility that orchestrates the release of YAMLScript, including 12 binary
    builds and `libyamlscript.so` bindings for 10 programming languages.
  * [util/brew-update](
    https://github.com/yaml/yamlscript/blob/main/util/brew-update)
    The utility that updates the Homebrew formula for YAMLScript.
  * [util/markys](
    https://github.com/yaml/yamlscript/blob/main/util/markys)
    Renders Markdown with embedded YAMLScript code blocks.
    Used for the YAMLScript documentation.
  * [util/version-bump](
    https://github.com/yaml/yamlscript/blob/main/util/version-bump)
    Bumps the version of dozens of YAMLScript files in the repository at
    release time.


More examples will be added here as they become known.

If you have YAMLScript example code you'd like to share, please submit a PR to
the [YAMLScript Repo](https://github.com/yaml/yamlscript).
