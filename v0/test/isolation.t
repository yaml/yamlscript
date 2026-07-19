#!/usr/bin/env bash

# Tests that the ys.v0 jar is self-contained: it ships only ys/v0
# namespaces and loads with just its declared dependencies (no compiler,
# no SCI, no core/src sources).

set -u

root=$(cd "$(dirname "$0")/../.." && pwd -P)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

count=0
check() {
  local got=$1 want=$2 name=$3
  count=$((count+1))
  if [[ $got == "$want" ]]; then
    echo "ok $count - $name"
  else
    echo "not ok $count - $name"
    echo "# got:  '$got'"
    echo "# want: '$want'"
  fi
}

echo '1..3'

cd "$root/v0" || exit 1

jar=$(ls target/ys.v0-*.jar 2>/dev/null | grep -v sources | head -1)
if [[ -z $jar ]]; then
  lein jar >/dev/null 2>&1
  jar=$(ls target/ys.v0-*.jar 2>/dev/null | grep -v sources | head -1)
fi
check "$([[ -n $jar ]] && echo found)" 'found' 'jar builds'

# Jar must contain only ys/v0 code (plus jar metadata)
stray=$(unzip -l "$jar" 2>/dev/null |
  awk '{print $4}' |
  grep '\.clj[cs]\?$' |
  grep -v '^META-INF/' |
  grep -cv '^ys/v0' || true)
check "$stray" '0' 'jar contains only ys/v0 namespaces'

# ys.v0 loads with only the jar + declared deps on the classpath
deps=$(lein classpath 2>/dev/null | tail -1 | tr ':' '\n' |
  grep '\.jar$' | paste -sd:)
got=$(bb --classpath "$jar:$deps" -e \
  "(ns main (:require ys.v0)) (ys.v0/init) (say (sum (rng 1 10)))" 2>&1)
check "$got" '55' 'jar loads standalone under bb'
