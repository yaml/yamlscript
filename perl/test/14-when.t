use YAMLScript::Test;

test_ys_to_ly <<'...';
- - |
    when:
    - a
    - 1
    - 2
  - (when a 1 2)

- - |
    a ?:
    - 1
    - 2
  - (when a 1 2)

- - |
    (a > b)?:
    - 1
    - 2
  - (when (> a b) 1 2)

- - |
    (a > b)|:
    - 1
    - 2
  - (when-not (> a b) 1 2)
...
