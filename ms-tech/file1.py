import yamlscript, json

ys = yamlscript.YAMLScript()

yaml = """
!ys-0:

me  :: ENV.USER:uc1

you ::
  base64-decode: 'TWljcm9zb2Z0'
"""

print(json.dumps(ys.load(yaml), indent=2))














