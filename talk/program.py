import yamlscript

ys = yamlscript.YAMLScript()

text = open('file.ys').read()

data = ys.load(text)

print(data)
