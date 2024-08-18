#!/usr/bin/env bash

source test/init

w='[a-zA-Z0-9_]'


AV=$ROOT/sample/advent

cmd="ys $AV/madlibs"
like "$($cmd)" \
"Dear $w+,

You should go to $w+( $w+)?.
I really think you would $w+ it there.

Sincerely, $w+" \
  "$cmd"

got=$(
cat <<'...' | ys -
!yamlscript/v0
name =: 'World'
say: "Hello $name. The answer is $(43 - 1)."
...
)
is "$got" "Hello World. The answer is 42." \
  "Interpolation example"

# got=$(
#   python -c \
#     'from yamlscript import YAMLScript; print(YAMLScript().load("Advent day: 3"))'
# )
# is "$got" "{'Advent day': 3}" "Python one liner"

done-testing
