---
title: Query and Transform YAML/JSON Files
---

You can use the `ys` command line tool to query and transform YAML and JSON
files.

```
# Convert YAML to JSON
$ ys -J file.yaml

# Convert JSON to YAML
$ ys -Y file.json

# Query a YAML file and reverse the result and print as JSON
$ ys -Je '.foo.0.bar:reverse' - < file.yaml
```

More examples soon.
