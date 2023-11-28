#!/usr/bin/env bash

set -e

ROOT=$(cd -P "$(dirname "${BASH_SOURCE[0]}")/../.."; pwd -P)

cd "$ROOT" || exit

export PATH=$ROOT/ys/bin:$PATH

set -x

[[ -x $ROOT/ys/bin/ys ]]

ys --help

ys --version

cat "$ROOT/test/hello.ys"

ys "$ROOT/test/hello.ys"

ys --load "$ROOT/test/hello.ys"

ys --eval 'say: "Hello, World!"'

ys -e 'say: "Hello, World!"' -e 'identity: 12345'

ys -e 'say: "Hello, World!"' -e 'identity: 12345' "$ROOT/test/hello.ys"

ys -pe 'range: 25'

ys -le 'range: 25'

ys -ms -le 'range: 25'

ys -ms -Ye 'range: 10'

ys -ms -Ee 'range: 30'

ys --compile "$ROOT/test/hello.ys"

ys -ce 'say: "Hello, World!"'

ys -c -e 'say: "Hello, World!"' -e 'identity: 12345'

ys -c -e 'say: "Hello, World!"' \
      -e 'say: "YAMLScript!!"' \
      "$ROOT/test/hello.ys"

ys -c -x parse -x build -x print -e 'say: "Hello, World!"'

ys -x all -e 'say: inc(41)'

echo 42 | ys -E

echo 42 | ys -E -

ys <(echo 'say: 333') <<<'say: 222' -e 'say: 111'

ys -Y "$ROOT/test/loader.ys"
