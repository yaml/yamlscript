#!/usr/bin/env bash

set -euo pipefail

[[ ${YS_SH_DEBUG-} ]] && set -x

yamlscript_version=0.2.32

main() (
  setup "$@"
  "do-$command" "${arguments[@]}"
)

do-install() (
  curl -sS https://yamlscript.org/install |
    PREFIX=${PREFIX:-$(dirname "$bindir")} LIB=1 bash
)

do-upgrade() (
  curl -sS https://yamlscript.org/install |
    PREFIX=${PREFIX:-$(dirname "$bindir")} bash
)

# Install the jars that Java-free 'ys -T bb' scripts load from ~/.m2
# under Babashka.
do-install-m2() (
  data_json_version=2.4.0

  install-m2-file \
    "org/yamlscript/ys.v0/$yamlscript_version" \
    "ys.v0-$yamlscript_version" \
    https://repo.clojars.org

  install-m2-file \
    "org/clojure/data.json/$data_json_version" \
    "data.json-$data_json_version" \
    https://repo1.maven.org/maven2

  jar=$HOME/.m2/repository/org/yamlscript/ys.v0
  jar+=/$yamlscript_version/ys.v0-$yamlscript_version.jar
  if [[ ! -e $jar.d ]] && command -v unzip >/dev/null; then
    unzip -o -q "$jar" -d "$jar.d"
    echo "Extracted: $jar.d"
  fi
)

install-m2-file() (
  path=$1 name=$2 repo=$3
  dir=$HOME/.m2/repository/$path
  mkdir -p "$dir"

  for file in "$name.jar" "$name.pom"; do
    if [[ -e $dir/$file ]]; then
      echo "Already installed: $dir/$file"
    else
      curl -fsSL -o "$dir/$file" "$repo/$path/$file" ||
        die "Failed to download: $repo/$path/$file"
      echo "Installed: $dir/$file"
    fi
  done
)

setup() {
  [[ ${1-} == --* ]] || die "Usage: --<command> [args...]"

  command=${1#--}
  shift
  arguments=("$@")
  bindir=$(cd -P "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)

  [[ $(command -v "do-$command") ]] ||
    die "Unknown command: --$command"
}

die() {
  printf '%s\n' "$@" >&2
  exit 1
}

[[ $0 != "${BASH_SOURCE[0]}" ]] || main "$@"
