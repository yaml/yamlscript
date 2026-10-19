#!/usr/bin/env bash

# Tests that the ys.v0 jar ships the runtime and taptest's regex definitions
# and loads with just its declared dependencies, without core/src sources.

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

echo '1..5'

cd "$root/v0" || exit 1

if ! lein jar >"$tmp/build.log" 2>&1; then
  cat "$tmp/build.log"
  exit 1
fi
version=$(perl -ne 'print $1 if /^\(defproject \S+ "([^"]+)"/' project.clj)
jar=target/ys.v0-$version.jar
check "$([[ -f $jar ]] && echo found)" found 'jar builds'

# Jar must exclude compiler code apart from the shared regex definitions.
entries=$(unzip -Z1 "$jar")
stray=$(printf '%s\n' "$entries" |
  grep '\.clj[cs]\?$' |
  grep -v '^META-INF/' |
  grep -cvE '^(ys/v0|yamlscript/regex\.cljc$)' || true)
check "$stray" 0 'jar excludes compiler namespaces'
check "$(printf '%s\n' "$entries" | grep -Fcx yamlscript/regex.cljc)" \
  1 'jar includes shared regex definitions'

# ys.v0 loads with only the jar + declared deps on the classpath
pathsep=:
[[ $(uname -s) =~ ^(MINGW|MSYS) ]] && pathsep=';'
deps=$(lein classpath 2>/dev/null | tail -1 | tr "$pathsep" '\n' |
  grep '\.jar$' | paste -sd"$pathsep" -)
program='(require (quote [clojure.java.io :as io])
                  (quote ys.v0) (quote ys.v0.taptest))
  (doseq [path ["ys/v0/std.cljc" "yamlscript/regex.cljc"]]
    (assert (= "jar" (.getProtocol (io/resource path)))))
  (ys.v0/init)
  (clojure.core/prn [(sum (rng 1 10)) (round -1.5) (floor -1.5)
        (pow 2 10) (pow 4 0.5) (sqrt 81)])'
want='[55 -1 -2.0 1024 2.0 9.0]'
got=$(bb --classpath "$jar$pathsep$deps" -e "$program" 2>&1)
check "$got" "$want" 'jar loads standalone under bb'
got=$(java -cp "$jar$pathsep$deps" clojure.main -e "$program" 2>&1)
check "$got" "$want" 'jar loads standalone under JVM Clojure'
