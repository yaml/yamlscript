#! bash

YAMLSCRIPT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" && pwd -P)

[[ $PATH == *$YAMLSCRIPT_ROOT/util:* ]] ||
  export PATH=$YAMLSCRIPT_ROOT/util:$PATH
