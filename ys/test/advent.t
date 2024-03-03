#!/usr/bin/env bash

source test/init

AV=$ROOT/sample/advent


cmd="ys $AV/silly.ys"
like "$($cmd)" "\w+.*,.*\w+" "$cmd"


cmd="ys $AV/twas-a-bit"
has "$($cmd)" "Twas a bit before" "$cmd"


cmd="ys -Y $AV/grocery.yaml"
is "$($cmd)" "\
- bread
- fruits:
  - apple
  - banana
  - cherry
- milk" \
  "$cmd"


cmd="ys $AV/tree.ys"
is "$($cmd)" "\
     *
    ***
   *****
  *******
 *********
     *
     *" \
  "$cmd"


cmd="ys $AV/tree.ys 7"
is "$($cmd)" "\
       *
      ***
     *****
    *******
   *********
  ***********
 *************
       *
       *" \
  "$cmd"


cmd="ys -p $AV/hearsay.ys"
like "$($cmd)" \
  "I heard that @\S+ uses YAMLScript in their \w+ code!" \
  "$cmd"


cmd="ys $AV/lol.ys"
is "$($cmd)" $'1\n2\n3' "$cmd"


cmd="ys $AV/madlibs"
like "$($cmd)" \
"Dear \w+,

You should go to \w+( \w+)?.
I really think you would \w+ it there.

Sincerely, \w+" \
  "$cmd"



done-testing
