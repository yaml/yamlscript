#!/usr/bin/env bash

set -e

ROOT=$(cd -P "$(dirname "${BASH_SOURCE[0]}")/../.."; pwd -P)

cd "$ROOT" || exit

export PATH=$ROOT/ys/bin:$PATH

set -x

[[ -x $ROOT/ys/bin/ys ]]

ys --help

ys --version

cat "$ROOT/ys/test/hello.ys"

ys "$ROOT/ys/test/hello.ys"

ys --load "$ROOT/ys/test/hello.ys"

ys --eval 'say: "Hello, World!"'

ys -e 'say: "Hello, World!"' -e 'identity: 12345'

ys -e 'say: "Hello, World!"' -e 'identity: 12345' "$ROOT/ys/test/hello.ys"

ys -pe 'range: 25'

ys -le 'range: 25'

ys -mc -le 'range: 25'

ys -mc -Ye 'range: 10'

ys -mc -Ee 'range: 30'

ys --compile "$ROOT/ys/test/hello.ys"

ys -ce 'say: "Hello, World!"'

ys -c -e 'say: "Hello, World!"' -e 'identity: 12345'

ys -c -e 'say: "Hello, World!"' \
      -e 'say: "YAMLScript!!"' \
      "$ROOT/ys/test/hello.ys"

ys -c -D parse -D build -D print -e 'say: "Hello, World!"'

ys -D all -e 'say: inc(41)'

ys -d -e 'say: inc(41)'

echo 42 | ys -E

echo 42 | ys -E -

ys <(echo 'say: 333') <<<'say: 222' -e 'say: 111'

ys -Y "$ROOT/ys/test/loader.ys"
