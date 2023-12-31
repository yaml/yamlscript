require 'minitest/autorun'

require_relative '../../lib/yamlscript'

class YAMLScriptTest < Minitest::Test
  def setup
    @ys = YAMLScript.new
  end

  def test_load_yaml_data
    data = @ys.load('a: 1')

    assert_equal data['a'], 1
  end
end
