#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: test done

AV =: "$fs-dirname(FILE)/../../sample/advent"

w =: /\w+/


test::
- name: ys silly.ys
  cmnd:: "ys $AV/silly.ys"
  like: \w+.*,.*\w+

- name: ys twas-a-bit
  cmnd:: "ys $AV/twas-a-bit"
  have: Twas a bit before

- name: ys -p hearsay.ys
  cmnd:: "ys -p $AV/hearsay.ys"
  like: I heard that @\S+ uses YAMLScript in their \w+ code!

- name: ys lol.ys
  cmnd:: "ys $AV/lol.ys"
  want: "1\n2\n3\n"

- name: ys -Y grocery.yaml
  cmnd:: "ys -Y $AV/grocery.yaml"
  want: |
    - bread
    - fruits:
      - apple
      - banana
      - cherry
    - milk

- name: ys tree.ys
  cmnd:: "ys $AV/tree.ys"
  want: |2+
         *
        ***
       *****
      *******
     *********
         *
         *

- name: ys tree.ys 7
  cmnd:: "ys $AV/tree.ys 7"
  want: |2+
           *
          ***
         *****
        *******
       *********
      ***********
     *************
           *
           *

- name: ys madlibs
  cmnd:: "ys $AV/madlibs"
  like:: |
    Dear $w+,

    You should go to $w+( $w+)?.
    I really think you would $w+ it there.

    Sincerely, $w+


- cmnd: "ys -le 'map inc: range(1, 10)'"
  want: "[2,3,4,5,6,7,8,9,10]"

- cmnd: "ys -le 'map inc:' -e '  range: 1, 10'"
  want: "[2,3,4,5,6,7,8,9,10]"

- cmnd: "ys -le '=>: map(inc, range(1, 10))'"
  want: "[2,3,4,5,6,7,8,9,10]"

- cmnd: "ys -le '->>: range(1, 10), map(inc)'"
  want: "[2,3,4,5,6,7,8,9,10]"

- name: Interpolation example
  code: |
    name =: 'World'
    say: "Hello $name. The answer is $(43 - 1)."
  what: out
  want: Hello World. The answer is 42.

done:
