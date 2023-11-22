#!/usr/bin/env bash

set -e

ROOT=$(cd -P "$(dirname "${BASH_SOURCE[0]}")"; pwd -P)

cd "$ROOT" || exit

export PATH=$ROOT:$PATH

set -x

[[ -x ys ]]

ys --help

ys --version

cat ../test/hello.ys

ys ../test/hello.ys

ys --load ../test/hello.ys

ys --eval 'say: "Hello, World!"'

ys -e 'say: "Hello, World!"' -e 'identity: 12345'

ys -e 'say: "Hello, World!"' -e 'identity: 12345' ../test/hello.ys

ys -pe 'range: 25'

ys -le 'range: 25'

ys -ms -le 'range: 25'

ys -ms -Ye 'range: 10'

ys -ms -Ee 'range: 30'

ys --compile ../test/hello.ys

ys -ce 'say: "Hello, World!"'

ys -c -e 'say: "Hello, World!"' -e 'identity: 12345'

ys -c -e 'say: "Hello, World!"' \
      -e 'say: "YAMLScript!!"' \
      ../test/hello.ys

ys -c -x parse -x build -x print -e 'say: "Hello, World!"'

ys -x all -e 'say: inc(41)'

echo 42 | ys -E

echo 42 | ys -E -

ys <(echo 'say: 333') <<<'say: 222' -e 'say: 111'

ys -Y ../test/loader.ys
