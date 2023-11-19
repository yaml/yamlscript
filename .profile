#! bash

YAMLSCRIPT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd -P)

[[ :$PATH: == *:"$YAMLSCRIPT_ROOT/ys":* ]] ||
  PATH=$YAMLSCRIPT_ROOT/ys:$PATH

YS() (
  set -e
  cd "$YAMLSCRIPT_ROOT/ys"
  make jar
  java -jar target/uberjar/yamlscript.cli-0.1.0-SNAPSHOT-standalone.jar "$@"
)

alias mtv='make test v=1'
