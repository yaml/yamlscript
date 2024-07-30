#!/usr/bin/env bash

source test/init

read -r -d '' expect <<... || true
ok 1 - String fail
not ok 2 - Number fail
#   Failed test 'Number fail'
#          got: 42
#     expected: 43
ok 3 - Pass expecting false
not ok 4 - Fail expecting false
#   Failed test 'Fail expecting false'
#          got: true
#     expected: false
not ok 5 - Fail expecting nil
#   Failed test 'Fail expecting nil'
#          got: true
#     expected: nil
not ok 6 - Fail expecting nil as string
#   Failed test 'Fail expecting nil as string'
#          got: true
#     expected: "nil"
1..6
...

cmd='ys test/taptest-failures.ys'
is "$($cmd)" "$expect" "$cmd"

done-testing
