use YAMLScript::Test;

test <<'...', 5;
(+ 2 3)
...

test <<'...', 5;
(2 + 3)
...

test <<'...', 5;
- (2 + 3)
...

test <<'...', 333;
- x =: 123
- y =: 456
- (y - x)
...

test <<'...', 7;
add(x, y): (x + y)
do: add(3,4)
...
