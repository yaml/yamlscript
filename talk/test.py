from yamlscript import YAMLScript

script = """
!yamlscript/v0

=>::
  foo:
  - foo
  - true
  - ! 10 .. 1
  bar:: curl("https://yamlscript.org/some.yaml")
        .yaml/load().members.0
"""

ys = YAMLScript()

data = ys.load(script)

print(data)
