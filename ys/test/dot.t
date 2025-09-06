#!/usr/bin/env ys-0

use ys::taptest: :all

NIL =: nil

test::
- note: "Dot chain testing"

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

- note: "Dot notation on sets - symbol access"

# Test that dot notation works consistently with get() method on sets
- code: \{'a' 'b' 'c'}.b
  want: "b"
- code: \{'a' 'b' 'c'}.get("b")
  want: "b"

# Test that both return nil for non-existent elements
- code: \{'a' 'b' 'c'}.d
  want:: nil
- code: \{'a' 'b' 'c'}.get("d")
  want:: nil

# Test with numeric access (should still work)
- code: \{5 4 3 2 1}.3
  want: 3
- code: \{5 4 3 2 1}.get(3)
  want: 3

# Test with different set contents
- code: \{'x' 'y' 'z'}.y
  want: "y"
- code: \{'x' 'y' 'z'}.get("y")
  want: "y"

# Test with mixed content sets
- code: \{1 'a' 2 'b'}.a
  want: "a"
- code: \{1 'a' 2 'b'}.get("a")
  want: "a"

# Test that numeric access still works on sets with numbers
- code: \{1 2 3}.1
  want: 1
- code: \{1 2 3}.get(1)
  want: 1

# Additional edge cases for dot notation on sets
- code: \{'a' 'b' 'c'}.a
  want: "a"
- code: \{'a' 'b' 'c'}.c
  want: "c"
- code: \{'a' 'b' 'c'}.get("a")
  want: "a"
- code: \{'a' 'b' 'c'}.get("c")
  want: "c"

# Test with empty set
- code: \{}.nonexistent
  want:: nil
- code: \{}.get("anything")
  want:: nil

# Test with single element set
- code: \{'single'}.single
  want: "single"
- code: \{'single'}.get("single")
  want: "single"

# Test with sets containing symbols vs strings
- code: \{\'a \'b \'c}.a
  want:: \'a
- code: \{\'a \'b \'c}.get(\'a)
  want:: \'a

# Test consistency across different set creation methods
- code: set(qw(a b c)).b
  want: "b"
- code: set(qw(a b c)).get("b")
  want: "b"

done:
