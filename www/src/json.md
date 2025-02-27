---
title: JSON is YAML!
talk: 0
---

Since [YAML 1.2](https://yaml.org/spec/1.2/spec.html) (2009) JSON is a complete
subset of YAML.
This means that any valid JSON file is also a valid YAML file and can be loaded
by a YAML loader to produce the equivalent data structure as if it were loaded
by a JSON parser.

Since YS is a YAML loader it can used on JSON files as well.

This is very useful for converting between the two formats and for using YS to
[query and transform](doc/query.md) JSON files.


!!! note

    Just because the YAML spec says it's a subset doesn't mean that all YAML
    tools can be trusted to handle JSON files 100% correctly.
    Caveat emptor.
