#! bash

YAMLSCRIPT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd -P)

[[ :$PATH: == *:"$YAMLSCRIPT_ROOT/ys/bin":* ]] ||
  PATH=$YAMLSCRIPT_ROOT/ys/bin:$PATH

YS() (
  set -e
  base=$YAMLSCRIPT_ROOT/ys
  jar=yamlscript.cli-0.1.30-SNAPSHOT-standalone.jar
  make --no-print-directory -C "$base" jar
  java -jar "$base/target/uberjar/$jar" "$@"
)

alias mtv='time -p make test v=1'
