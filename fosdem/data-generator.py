import yamlscript, json

ys = yamlscript.YAMLScript()

program = open('data-generator.ys').read()

data = ys.load(program)

print(json.dumps(data))
