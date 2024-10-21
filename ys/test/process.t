#!/usr/bin/env ys-0

require ys::taptest: :all

env-update::
  FOO_BAR: BAZ

test::
- code: |
    sh-out:: bash -c 'echo $FOO_BAR'
  want: BAZ

- code: |
    sh-out {:dir '..'}:: bash -c 'echo $FOO_BAR'
  want: BAZ

- code: |
    sh("bash -c 'echo \$FOO_BAR'").out:chomp
  want: BAZ

- code: |
    sh({:dir '..'} 'bash -c "echo $FOO_BAR"').out:chomp
  want: BAZ

done: 4
