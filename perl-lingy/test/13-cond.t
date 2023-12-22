use YAMLScript::Test;

test_ys_to_ly <<'...';
- - |
    cond:
    - a
    - 1
    - b
    - 2
  - (cond a 1 b 2)

- - |
    ???:
    - a
    - 1
    - b
    - 2
  - (cond a 1 b 2)

- - |
    ???:
      a: 1
      b: 2
  - (cond a 1 b 2)
...
