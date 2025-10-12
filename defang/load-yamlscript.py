import sys, json, yamlscript
ys = yamlscript.YAMLScript()

with open(sys.argv[1], 'r') as file:
    try:
        data = ys.load(file.read())

    except Exception as exc:
        print(exc)

print(json.dumps(data, indent=2))

