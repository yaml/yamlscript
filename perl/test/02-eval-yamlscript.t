use YAMLScript::Test;

test_eval <<'...';
- - (+ 2 3)
  - 5

- - (2 + 3)
  - 5

- - '- (2 + 3)'
  - 5

- - |
    - x =: 123
    - y =: 456
    - (y - x)
  - 333

- - |
    defn add(x, y): (x + y)
    do: add(3,4)
  - 7
...
