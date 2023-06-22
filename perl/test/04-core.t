use YAMLScript::Test;

test_eval <<'...';
- - |
    ends-with?("foo" "bar")
  - 'false'
- - |
    ends-with?("foo.ys" ".ys")
  - 'true'

...
