---
title: YS TAP Testing Library
talk: 0
---

This library provides an implementation of the [Test Anything Protocol (TAP)](
https://testanything.org/) for YS.

It's a really clean way to write unit tests for your YS code.

Here's an example of a simple test file (test-1.t):

```yaml
#!/usr/bin/env ys-0

require ys::taptest: test done

hash1 =::
  foo: one
  bar: two

hash2 =::
  bar: three

test::
- name: Test merge
  code: hash1.merge(hash2)
  want:
    foo: one
    bar: three

- name: Count keys
  code: hash1.keys().count()
  want: 2

done: 2     # Make sure we ran 2 tests
```

We can run this test file with the common `prove` command:

```sh
$ prove -v test-1.t
test-1.t ..
ok 1 - Test merge
ok 2 - Count keys
1..2
ok
All tests successful.
Files=1, Tests=2,  0 wallclock secs ( 0.01 usr  0.00 sys +  0.01 cusr  0.00 csys =  0.02 CPU)
Result: PASS
```

Tests are defined as a simple YAML sequence of mappings, one for each test.
The `test` functions takes this sequence and runs each test in order.

TAP needs to know that all the tests you intended to run were run.
You can do this by calling the `plan` function beforehand with the number of
tests you intend to run, or by calling the `done` afterwards with the number of
tests you intended to run.
You can also call `done` with no arguments (`done:`) to indicate that all tests
were run.


## Test Mappings

Each test mapping has certain keys that determine how the test is run.

You must specify `code` or `cmnd` for each test.
This indicates either the YS code to run or the CLI command to run.

You must specify one of `want`, `like` or `have` for each test.
This indicates how the result should be tested.

Here are the test mapping keys you can use in a test:

* `name` — The name/description of the test. This is optional.

* `code` — The YS code to run for the test.

* `cmnd` — The CLI command to run for the test.

* `want` — The exact expected result of the test.

* `like` — A regex pattern that the result should match.

* `have` — A substring that the result should contain.

* `what` — What part of the result to test.
  Can be set to `value`, `error` or `out` for `code` tests and defaults to
  `value`.
  Can be set to `out`, `err`, `exit` or `all` for `cmnd` tests and defaults to
  `out`.
  See below for more information on these `what` values.

* `form` — A function to format the result for testing.
  The function will be called with the full result data and the current test
  object.

* `SKIP` — If set to `true`, this test will be skipped.
  Can be used on multiple tests.

* `ONLY` — If set to `true` only this test will be run.
  Can be used on multiple tests.


### The `what` Values

* `value` — Test the return value of the code.
  This is the default for `code` tests.

* `error` — Test the error message of the code.
  This expects the code to throw an error.

* `out` — Test the stdout of the command.

* `err` — Test the stderr of the command.

* `exit` — Test the exit code of the command.

* `all` — An object containing `out`, `err` and `exit` values.


## The ys::taptest API Functions

* `done` — Indicate that a certain number of tests were run.
  Run this after running all tests.

* `plan` — Plan to run a certain number of tests.
  Run this before running any tests.

* `test` — Run a sequence of tests.
