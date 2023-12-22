use YAMLScript::Test;

test_ys_to_ly <<'...';
- - |
    foo: []
  - (foo)

- - |
    foo:
  - /Use 'foo\(\):' for a call with no args/

- - |
    foo():
  - (foo)

- - |
    foo: [a, b, c]
  - (foo a b c)

- - |
    foo:
    - a
    - b
    - c
  - (foo a b c)

- - |
    foo(a b c):
  - (foo a b c)

- - |
    foo: a b c
  - (foo a b c)

- - |
    foo: (a b c)
  - (foo (a b c))

- - |
    foo: 'a b c'
  - (foo "a b c")

- - |
    foo(a): b c
  - (foo a b c)

- - |
    foo(a): (b c)
  - (foo a (b c))

- - |
    foo(a, b): c
  - (foo a b c)

- - |
    foo(a): b (c + 1)
  - (foo a b (+ c 1))

- - |
    foo:
    - a
    - b: c
  - (foo a (b c))

- - |
    foo(a):
    - b: c
  - (foo a (b c))

- - |
    foo(a):
      b: c
  - (foo a (b c))

- - |
    foo(a):
      b: c
      d: e
  - (foo a (do (b c) (d e)))

- - |
    foo(a):
    - b: c
    - d: e
  - (foo a (b c) (d e))

- - |
    foo((a * 123), -): (
      b
      +
      1
      )
      (
      c
      /
      2
      )
  - (foo (* a 123) - (+ b 1) (/ c 2))
...
