use YAMLScript::Test;

test_ys_to_ly <<'...';

- - '(x, y): (+ y x)'
  - (fn* [x y] (+ y x))

- - |
    ad =:
    - (x, y): (+ x y)
  - (def ad (fn* [x y] (+ x y)))
...
