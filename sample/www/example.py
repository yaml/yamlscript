import yamlscript
ys = yamlscript.YAMLScript()
text = open("db-config.yaml").read()
data = ys.load(text)
