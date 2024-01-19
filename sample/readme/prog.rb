require 'yamlscript'
input = IO.read('file.ys')
ys = YAMLScript.new
data = ys.load(input)
puts data
