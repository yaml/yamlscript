#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

NIL =: nil

base =:
  if CWD =~ /\/yamlscript$/:
    then: "$CWD/ys"
    else: CWD

test::
#-------------------------------------------------------------------------------
- note: 'Special functions'

- name: Vector def destructuring
  code: |
    -[what who] =: qw(Hello World)
    =>: "$what $who"
  want: Hello World

- name: Mapping def destructuring
  code: |
    -{:strs [what who]} =: -{"what" "Hello" "who" "World"}
    =>: "$what $who"
  want: Hello World


#-------------------------------------------------------------------------------
- note: 'Short named alias functions'

# a and just
- code: a(41).inc()
  want: 42

- code: len('hello')
  want: 5
- code: -'Hello'.len()
  want: 5

# q is for quote
- code: q(name)
  want:: \'name
- code: q((1 2 3))
  want:: \'(1 2 3)

- code: value('inc')
  want:: inc
- code: value('inc').call(41)
  want: 42
- code: value(q(inc))
  want:: inc

- code: qw(one :two "three" 4 true false nil {} [])
  want:: -['one' ':two' "three" '4' 'true' 'false' 'nil' '{}' '[]']



#-------------------------------------------------------------------------------
- note: "Named function aliases for infix operators"

- code: eq(23 23 23 23)
- code: eq("x" "x" "x" "x")
- code: eq(["x" "x"] ["x" "x"])
- code: eq({"x" "x"} {"x" "x"})
- code: eq(false not(true))
- code: eq(nil first([]) nil)
- code: (1 .. 10).filter(gt(5))
  want:: -[6 7 8 9 10]

- code: ne(23 23 23 24)
- code: ne("x" "x" "x" "y")
- code: ne(["x" "x"] ["x" "y"])
- code: ne({"x" "x"} {"x" "y"})
- code: ne(false not(true).not())
- code: ne(nil [] nil)

- code: 'gt: (2 + 3) 4'
- code: 'ge: (2 + 2) 4'
- code: 'lt: 4 (2 + 3)'
- code: 'le: 4 (2 + 2)'
- code: 'lt: 1 2 3 4'
- code: 'le: 1 2 2 3'


#-------------------------------------------------------------------------------
- note: "Truthy and falsy operations"

- code: falsey?(0)
- code: falsey?(0.0)
- code: falsey?('')
- code: falsey?("")
- code: falsey?([])
- code: falsey?({})
- code: falsey?(\{})
- code: falsey?(nil)
- code: falsey?(false)

- code: ("" ||| [] ||| 42) == 42
- code: (42 &&& []) == nil


#-------------------------------------------------------------------------------
- note: "Common type conversion functions"

- code: to-num("42") == 42
- code: to-num(42) == 42
- code: to-num(1 .. 42) == 42
- code: to-num(set(1 .. 42)) == 42
- code: to-num(to-map(1 .. 42)) == 21
- code: to-num(to-vec(to-map(1 .. 42))) == 42
- code: to-num("") == nil
- code: to-num("xyz") == nil
- code: to-vec(()) == []


#-------------------------------------------------------------------------------
- note: "Math functions"

- code: 4 ** 3 ** 2
  want: 262144

- code: 1 + \\A
  want: 66

- code: sum(3 .. 9)
  want: 42

- code: sum([3 nil 4])
  want: 7

- code: 5 / 2
  want: 2.5
- code: 6 / 2
  want: 3

- code: digits(90210)
  want:: \'(9 0 2 1 0)
- code: digits("90210")
  want:: \'(9 0 2 1 0)
- code: digits("012345")
  want:: \'(0 1 2 3 4 5)
- code: digits(012345)
  want:: \'(1 2 3 4 5)


#-------------------------------------------------------------------------------
- note: "YAML Anchor and alias functions"


#-------------------------------------------------------------------------------
- note: "YAMLScript document result stashing functions"


#-------------------------------------------------------------------------------
- note: "Dot chaining support"

- code: nil.$NIL
  want: null
- code: nil.123
  want: null
- code: nil.foo
  want: null
- code: -{}.$NIL
  want: null
- code: -[].$NIL
  want: null
- code: true.foo
  want: null
- code: -"foo".foo
  want: null

- code: (1 .. 20).partition(3 5)
  want:: \'((1 2 3) (6 7 8) (11 12 13) (16 17 18))


#-------------------------------------------------------------------------------
- note: "Control functions"

# call a function by reference, string, or symbol
- code: -'inc'.call(41)
  want: 42
- code: \'inc.call(41)
  want: 42
- code: |
    ns: foo
    -"inc": .call(41)
  want: 42
- code: |
    each x (1 .. 3): x.++
  want:: \'(2 3 4)

- code: die()
  what: error
  want: Died


#-------------------------------------------------------------------------------
- note: "String functions"

- code: ('foo' == 'oof'.reverse())
- code: ('foo' != 'bar')

- code: uc('foo') == 'FOO'
- code: uc1('foo') == 'Foo'
- code: lc('FoOoO') == 'foooo'

- code: -"Hello".split().join()
  want: Hello

- code: chop('hello')
  want: hell
- code: chop(3 'hello')
  want: he
- code: -'howdy'.chop(2)
  want: how

- code: -{\\o "xyz"}.escape("foo")
  want: fxyzxyz


#-------------------------------------------------------------------------------
- note: "Collection functions"

- code: (\\A .. \\E)
  want:: \'(\\A \\B \\C \\D \\E)

- code: 'reduce + 0 (1 .. 5):'
  want: 15
- code: 'reduce _ 0 (1 .. 5): +'
  want: 15
- code: -[{"a" 1}{"a" 2}].map(\(get _ "a"))
  want:: \'(1 2)
- code: -"abc".map(int)
  want:: \'(97 98 99)
- code: int.map('abc')
  want:: \'(97 98 99)
- code: -"abc".map(int).mapv(inc)
  want:: -[98 99 100]
- code: '(1 .. 10).has?(5)'
- code: 'num(5).in?(1 .. 10)'

- code: -"ello".cons(\\H).join()
  want: Hello
- code: -[[1 2] [3 4]].mapcat(reverse)
  want:: -[2 1 4 3]
- code: (1 .. 5).reduce(+)
  want: 15
- code: (1 .. 5).reduce(+ 10)
  want: 25


#-------------------------------------------------------------------------------
- note: "I/O functions"


#-------------------------------------------------------------------------------
- note: "File system functions"

- code: 'fs-d: CWD'
- code: 'fs-e: CWD'
- code: 'fs-f: "$base/test/std.t"'
- code: 'fs-l: "$base/test/a-symlink"'
- code: 'fs-r: CWD'
- code: 'fs-s: CWD'
- code: 'fs-w: CWD'
- code: 'fs-x: CWD'
- code: 'fs-z: "$base/test/empty-file"'

- code: fs/cwd().str()
  want:: CWD
- code: 'fs-which: "ys"'
  like: /ys$
- code: fs-mtime(CWD).str()
  like: ^\d{13}$


#-------------------------------------------------------------------------------
- note: "Regex functions"

- code: -"I Like Pie".split().filter(\(/[A-Z]/)).join()
  want: ILP

- code: -'Hello World'.replace('o')
  want: Hell Wrld
- code: -'Hello World'.replace(/[lo]/)
  want: He Wrd


#-------------------------------------------------------------------------------
- note: "Java interop functions"


#-------------------------------------------------------------------------------
- note: "IPC functions"


#-------------------------------------------------------------------------------
- note: "External library functions"


#-------------------------------------------------------------------------------
- note: "HTTP functions"


#-------------------------------------------------------------------------------
done:
