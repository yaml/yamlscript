#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

# Load the hello-world YS module:
use: 'hello-world'

# Define a custom form function:
defn inc2(n _):
  =>: n + 2


# Run the tests:
test::
- name: Check exact return value
  code: hello-world/hello()
  want: 'Hello, world!'

- name: Check return value has substring
  code: hello-world/hello()
  have: 'Hello,'

- name: Number test data-mode
  code: 6 * 7
  want: 42

- name: Number test code-mode
  code: 3 + 4 + 5 + 6 + 7 + 8 + 9
  want:: 42

- name: False test data-mode
  code: 1 = 2
  want: false

- name: False test code-mode
  code: 1 = 2
  want:: false

- name: Nil test null
  code: nil
  want: null

- name: Nil test empty
  code: nil
  want:

- name: Nil test nil
  code: nil
  want:: nil

- name: Nil test first
  code: first([])
  want:: nil

- name: Nil test rest
  code: rest([])
  want:: ()

- name: Division by zero
  code: 1 / 0
  what: error
  have: Divide by zero

- name: Check stdout
  code: |
    say: 'Hello, world!'
  what: out
  want: "Hello, world!\n"

- name: Run command return stdout
  cmnd: echo 'Hello, world!'
  want: "Hello, world!\n"

- name: Run command return exit code
  cmnd: echo 'Hello, world!'
  what: exit
  want: 0

- name: Run command return all
  cmnd: echo 'Hello, world!'
  what: all
  want:
    exit: 0
    out: "Hello, world!\n"
    err: ''

- name: Use form on return value
  code: '41'
  form:: \(inc %1)
  want: 42

- name: Use a previously defined form function on return value
  code: '40'
  form:: inc2
  want: 42

- name: Use form function on return value
  code: '41'
  form::
    fn [val test]:
      inc: val
  want: 42

- name: Use form function on error test
  code: 1 / 0
  form::
    fn [e _]: e.cause
  want: Divide by zero

- name: Use form function on a command
  cmnd: echo 'Hello, world!'
  form::
    fn [val _]: val.out
  want: "Hello, world!\n"


# Declare we are done with the number of tests:
done: 21
