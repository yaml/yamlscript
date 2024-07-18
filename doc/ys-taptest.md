---
title: YAMLScript TAP Testing Library
---

This library provides an implementation of the [Test Anything Protocol (TAP)](
https://testanything.org/) for YAMLScript.

It's a really clean way to write unit tests for your YAMLScript code.

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
This indicates either the YAMLScript code to run or the CLI command to run.

You must specify one of `want`, `like` or `have` for each test.
This indicates how the result should be tested.

Here are the test mapping keys you can use in a test:

* `name` — The name/description of the test. This is optional.

* `code` — The YAMLScript code to run for the test.

* `cmnd` — The CLI command to run for the test.

* `want` — The exact expected result of the test.

* `like` — A regex pattern that the result should match.

* `have` — A substring that the result should contain.

* `take` — Must be set to `out` to test the output of a CLI command.  
  NOTE: Many more options in next release.

* `fail` — If set to `true`, the test is expected to fail.  
  NOTE: Will be replaced by `take: error` in the next release.

<!--
* `take` — Must be set to one of:
  * `out` — Test the stdout of a CLI command.
  * `err` — Test the stderr output of a CLI command.
  * `rc` — Test the return code of a CLI command.
  * `all` — Test all of (`out`, `err`, `rc`) in a mapping.
  * `error` — Expect code to error and test the error message.
* `ONLY` — If set to `true`, only this test will be run.
* `SKIP` — If set to `true`, this test will be skipped.
-->


## Functions

* `done` — Indicate that a certain number of tests were run.
  Run this after running all tests.

* `plan` — Plan to run a certain number of tests.
  Run this before running any tests.

* `test` — Run a sequence of tests.
