#! bash

YAMLSCRIPT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd -P)

[[ :$PATH: == *:"$YAMLSCRIPT_ROOT/ys":* ]] ||
  PATH=$YAMLSCRIPT_ROOT/ys:$PATH

YS() (
  set -e
  base=$YAMLSCRIPT_ROOT/ys
  jar=yamlscript.cli-0.1.0-SNAPSHOT-standalone.jar
  make --no-print-directory -C "$base" jar
  java -jar "$base/target/uberjar/$jar" "$@"
)

alias mtv='time make test v=1'
