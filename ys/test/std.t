#!/usr/bin/env ys-0

use ys::taptest: :all

NIL =: nil

base =:
  if CWD =~ /\/yamlscript$/:
    then: "$CWD/ys"
    else: CWD

test::
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

- code: nil.in?([1 nil 3])

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

- code: qw(foo bar baz).index('bar')
  want: 1

- code: -{1 2 3 4}.3
  want: 4


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

- code: eval('sum(3 .. 9)')
  want: 42


#-------------------------------------------------------------------------------
- note: "Function functions"

- code: |
    bus =: sub:flip
    bus: 2 44
  want: 42


#-------------------------------------------------------------------------------
- note: "Regex functions"

- code: -"I Like Pie".split().filter(\(/[A-Z]/)).join()
  want: ILP

- code: -'Hello World'.replace('o')
  want: Hell Wrld
- code: -'Hello World'.replace(/[lo]/)
  want: He Wrd


#-------------------------------------------------------------------------------
- note: "I/O functions"


#-------------------------------------------------------------------------------
- note: 'Shorter named alias functions'

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
  want:: -['one' ':two' "three" '4' 'true' 'false' 'nil' '(%)' '[]']


#-------------------------------------------------------------------------------
- note: "Quoting functions"


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
- note: "Common type casting functions"

- code: to-char(nil)
  what: error
  want: Can't convert nil to char

- code: to-num("42") == 42
- code: to-num(42) == 42
- code: to-num(1 .. 42) == 42
- code: to-num(set(1 .. 42)) == 42
- code: to-num(to-map(1 .. 42)) == 21
- code: to-num(to-vec(to-map(1 .. 42))) == 42
- code: to-num("") == nil
- code: to-num("xyz") == nil

- code: to-list([]) == \'()
- code: to-list({}) == \'()
- code: to-list('') == \'()
- code: to-list({:a 1}) == \'(:a 1)
- code: to-list(range(3)) == \'(0 1 2)
- code: to-list(1 .. 3) == \'(1 2 3)
- code: to-list('abc') == \'(\\a \\b \\c)
- code: to-list(42)
  what: error
  want: Can't convert int to list
- code: to-list(nil)
  what: error
  want: Can't convert nil to list

- code: to-vec(()) == []
- code: to-vec([]) == []
- code: to-vec(\'(1 2 3)) == [1 2 3]


#-------------------------------------------------------------------------------
- note: "Alternate truth functions"

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
- code: 'fs-basename: "$base/test/std.t"'
  want: std.t
- code: fs-basename('$base/test/std.t' 't')
  want: std
- code: fs-basename('$base/test/std.t' '*')
  want: std


#-------------------------------------------------------------------------------
- note: "Date/Time functions"


#-------------------------------------------------------------------------------
- note: "YAML anchor and alias functions"


#-------------------------------------------------------------------------------
- note: "Java interop functions"


#-------------------------------------------------------------------------------
- note: "Security functions"

- code: md5("foo\n")
  want: d3b07384d113edec49eaa6238ad5ff00
- code: sha1("foo\n")
  want: f1d2d2f924e986ac86fdf7b36c94bcdf32beec15
- code: sha256("foo\n")
  want: b5bb9d8014a0f9b1d61e21e796d78dccdf1352f23cd32812f4850b878ae4944c


#-------------------------------------------------------------------------------
- note: "IPC functions"


#-------------------------------------------------------------------------------
- note: "External library functions"


#-------------------------------------------------------------------------------
- note: "HTTP functions"


#-------------------------------------------------------------------------------
- note: "YS document result stashing functions"


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
done:
