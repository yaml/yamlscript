use YAMLScript::Test;

test_ys_to_ly <<'...';

- - .#"foo"
  - '#"foo"'


- - Regex DSL
  - /foo/
  - '#"foo"'


- - Dynamic symbol
  - .*foo*
  - '*foo*'


- - 'x =: 7'
  - (def x 7)


- - (1 + 2)
  - (+ 1 2)


- - prn(123 456)
  - (prn 123 456)


- - 'if (a = b): [a]'
  - (if (= a b) a)


- - Multiple ysexprs in one scalar
  - prn(123) prn(456)
  - (do (prn 123) (prn 456))


- - 'defn add(x, y): (x + y)'
  - (defn add [x y] (+ x y))


- - (ns Foo::Bar::Baz)
  - (ns Foo.Bar.Baz)


- - 'ns: Foo::Bar'
  - (ns Foo.Bar)


- - |
    require: .'Foo::Bar
  - (require (quote Foo.Bar))


- - 'defn main(n=99): nil'
  - (defn main [& _args_] (let* [n (nth _args_ 0 99)] nil))


- - 'defn foo(x, *xs): nil'
  - (defn foo [x & xs] nil)


- - foo((x y)) - defmacro
  - |
    defmacro todo(label, *tests):
      .`(.todo_skip t ~label)
  - (defmacro todo [label & tests] (quasiquote (.todo_skip t (unquote label))))


- - Nested sequences
  - |
    - - x =: 111
      - defn foo(x):
        - y =: inc(x)
        - (x * 5)
  - (do (def x 111) (defn foo [x] (let* [y (inc x)] (* x 5))))


- - Multi-arity defn
  - |
    defn add:
      (): 0
      (x): cast(Number x)
      (x, y): (x + y)
      (x, y, *more):
        (reduce + (x + y) more)
  - (defn add ([] 0) ([x] (cast Number x)) ([x y] (+ x y)) ([x y & more] (reduce + (+ x y) more)))


- - .`(.foo Abc::Def::Ghi t ~a)
  - (quasiquote (.foo Abc.Def.Ghi t (unquote a)))


- - 'use: Foo::Bar'
  - (use (quote Foo.Bar))


- - foo->bar(baz 123)
  - (. foo (bar baz 123))


- - (1 .. 10)
  - (-range 1 10)


# - - foo->bar()->baz()
#   - xxx


# # - - foo->bar(baz->zab(123))->aaa()
# - - a->b(c->d(123)->e)->aaa()
#   - xxx


# ns Test::More::YAMLScript:
#   use:
#   - Lingy::Util
#   import:
#   - Test::Builder


...
