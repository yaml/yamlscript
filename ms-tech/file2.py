import yamlscript, json

ys = yamlscript.YAMLScript()

# Works with plain YAML
yaml = """
me: Ingy
you: Microsoft
"""

print(json.dumps(ys.load(yaml), indent=2))




































