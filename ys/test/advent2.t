#!/usr/bin/env ys-0

!yamlscript/v0

require ys::taptest: test done

AV =: FILE.dirname() + '/../../sample/advent'

test::
- name: ys -Y sample/advent/grocery.yaml
  cmnd:: "ys -Y $AV/grocery.yaml"
  take: out
  want: |
    - bread
    - fruits:
      - apple
      - banana
      - cherry
    - milk

- name: ys sample/advent/tree.ys
  cmnd:: "ys $AV/tree.ys"
  take: out
  want: |2+
         *
        ***
       *****
      *******
     *********
         *
         *

- name: ys sample/advent/tree.ys 7
  cmnd:: "ys $AV/tree.ys 7"
  take: out
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

done:
