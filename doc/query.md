---
title: Convert/Query/Transform
talk: 0
---

You can use the `ys` command line tool to convert, query and transform YAML (or
JSON since [JSON is YAML](json.md)) files much like you would with
[`jq`](https://stedolan.github.io/jq/) or
[`yq`](https://mikefarah.gitbook.io/yq).

!!! note

    While `yq` currently has features that `ys` does not (like updating files
    in place and preserving comments), `ys` has a much larger set of functions
    (it's an entire programming language) to make use of.

    Both are fantastic tools and can be used together for great good!

YS also works with CSV, TSV and Clojure's EDN formats, with even more formats
planned for the future.


## YS Conversion Examples

* Convert YAML to JSON

    ```
    $ ys -l file.yaml  # Compact JSON output
    $ ys -J file.yaml  # Pretty JSON output
    $ ys -l file.yaml | jq .   # Pipe to jq for even prettier JSON output

    $ cat file.yaml | ys -l -  # Read from stdin (use - for file name)
    $ cat file.yaml | ys -l    # -l with no file argument reads from stdin
    ```

<!--
# Convert JSON to YAML

```
$ ys -Y file.json

# Query a YAML file and reverse the result and print as JSON
$ ys -Je '.foo.0.bar:reverse' - < file.yaml
```

More examples soon.
-->
