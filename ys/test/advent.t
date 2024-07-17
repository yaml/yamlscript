#!/usr/bin/env bash

source test/init

w='[a-zA-Z0-9_]'
S='[^ ]'


AV=$ROOT/sample/advent


cmd="ys $AV/silly.ys"
like "$($cmd)" "$w+.*,.*$w+" "$cmd"


cmd="ys $AV/twas-a-bit"
has "$($cmd)" "Twas a bit before" "$cmd"


cmd="ys -p $AV/hearsay.ys"
like "$($cmd)" \
  "I heard that @$S+ uses YAMLScript in their $w+ code!" \
  "$cmd"


cmd="ys $AV/lol.ys"
is "$($cmd)" $'1\n2\n3' "$cmd"


cmd="ys $AV/madlibs"
like "$($cmd)" \
"Dear $w+,

You should go to $w+( $w+)?.
I really think you would $w+ it there.

Sincerely, $w+" \
  "$cmd"

cmd="ys -le 'map inc: range(1, 10)'"
got=$(eval "$cmd")
is "$got" "[2,3,4,5,6,7,8,9,10]" "$cmd"

cmd="ys -le 'map inc:' -e '  range: 1, 10'"
got=$(eval "$cmd")
is "$got" "[2,3,4,5,6,7,8,9,10]" "$cmd"

cmd="ys -le '=>: map(inc, range(1, 10))'"
got=$(eval "$cmd")
is "$got" "[2,3,4,5,6,7,8,9,10]" "$cmd"

cmd="ys -le '->>: range(1, 10), map(inc)'"
got=$(eval "$cmd")
is "$got" "[2,3,4,5,6,7,8,9,10]" "$cmd"

got=$(
cat <<'...' | ys -
!yamlscript/v0
name =: 'World'
say: "Hello $name. The answer is $(43 - 1)."
...
)
is "$got" "Hello World. The answer is 42." \
  "Interpolation example"

got=$(
  python -c \
    'from yamlscript import YAMLScript; print(YAMLScript().load("Advent day: 3"))'
)
is "$got" "{'Advent day': 3}" "Python one liner"

done-testing
