---
title: Convert/Query/Transform
talk: 0
---

You can use the `ys` command line tool to convert, query and transform YAML (or
JSON since [JSON is YAML](../json.md)) files much like you would with
[`jq`](https://stedolan.github.io/jq/) or
[`yq`](https://mikefarah.gitbook.io/yq).

??? tip "[Install `ys` Now!](install.md)"

    ```
    curl -s https://getys.org/ys | bash
    ```



YS is an excellent tool for these types of CLI 1-liner tasks, because it:

* Is a full programming language with 100s of builtin functions
* Has a great compact "dot notation" syntax
* Has many options for output formats and other niceties

For example, to get the first 5 keys of a subsection of a YAML file that start
with a vowel and print the result as YAML to stdout:

```bash
$ ys '.some.0.part:keys.filter(/^[aeiou]/).take(5)' file.yaml
- id
- enabled
- owner
- interval
- environment
```

??? question "Should I use `yq` or `ys`?"

    Both!

    While `yq` currently has features that `ys` does not (like updating files
    in place and preserving comments), `ys` has a much larger set of functions
    (it's an entire programming language) to make use of.

    Both are fantastic tools and can be used together for great good!


## YS Conversion Examples

Click on the examples below to see more details.

* Convert YAML/JSON to other formats

    ??? "`ys -J file.yaml   # Convert to JSON`"
        Use one of these Load options:
        * `-l`/`--load` Load input and print as compact JSON
        * `-J`/`--json` Print as formatted JSON
        * `-Y`/`--yaml` Print as YAML
        * `--csv` Print as CSV
        * `--tsv` Print as TSV
        * `--edn` Print as EDN

        Prints the data in the specified format to stdout.

        Examples:
        ```
        ys -l file.yaml  # --load for compact JSON output
        ```
        ```
        ys -J file.yaml  # Pretty JSON output
        ```
        ```
        ys -l file.yaml | jq .   # Pipe to jq for even prettier JSON output
        ```
        ```
        < file.yaml ys -l -  # Read from stdin (use - for file name)
        ```
        ```
        < file.yaml ys -l    # -l with no file argument reads from stdin
        ```

* Show code evaluation in a format

    ??? "`ys -Ye ENV   # Environment as YAML`"

        ```
        ys -Ye ENV  # Environment as YAML
        ```
        ```
        ys -Ye 'ENV:sort:flat:O'  # Sorted
        ```

### _Many more examples soon..._

<!--
* "Convert JSON to YAML"

    ```
    $ ys -Y file.json

    # Query a YAML file and reverse the result and print as JSON
    $ ys -Je '.foo.0.bar:reverse' - < file.yaml
    ```
-->
