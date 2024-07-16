#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: :all

use: 'hello-world'

test::
- name: Hello world test
  code: hello-world/hello()
  want: 'Hello, world!'

- name: Double check!
  code: hello-world/hello()
  want: 'Hello, world!'

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
  fail: true
  have: Divide by zero

- name: Take stdout
  code: |
    say: 'Hello, world!'
  take: out
  want: "Hello, world!\n"

- name: Run command
  cmnd: echo 'Hello, world!'
  take: out
  want: "Hello, world!\n"

done: 13
