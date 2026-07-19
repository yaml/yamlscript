#!/usr/bin/env bash

# End-to-end tests: `ys -T clj` output runs under babashka.

set -u

root=$(cd "$(dirname "$0")/../.." && pwd -P)
ys=${YS_BIN:-$root/util/ysj}
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

# Full dependency classpath for bb; sources + dep jars:
cp=$(cd "$root/v0" && lein classpath 2>/dev/null | tail -1)
if [[ -z $cp ]]; then
  echo "1..0 # SKIP could not compute lein classpath"
  exit 0
fi

bb_run() { bb --classpath "$cp" "$@"; }

echo '1..7'

# The motivating example
"$ys" -T clj -e 'say: (1 .. 10):sum' 2>/dev/null > "$tmp/sum.clj"
check "$(bb_run "$tmp/sum.clj")" '55' 'sum pipeline'

# No warnings on stderr
err=$(bb_run "$tmp/sum.clj" 2>&1 >/dev/null)
check "$err" '' 'no warnings on stderr'

# defn main with args
cat > "$tmp/main.ys" <<'EOF'
!ys-0
defn main(name='World'):
  say: "Hello, $name!"
EOF
"$ys" -T clj "$tmp/main.ys" 2>/dev/null > "$tmp/main.clj"
check "$(bb_run "$tmp/main.clj" Bob)" 'Hello, Bob!' 'defn main with ARGS'
check "$(bb_run "$tmp/main.clj")" 'Hello, World!' 'defn main default args'

# Multi-doc code with +++, aliases and ordered maps
cat > "$tmp/multi.ys" <<'EOF'
!ys-0
say: str/upper-case('doc one')
--- !code
say: json/dump({'a' 1})
--- !code
say: omap('z' 1 'a' 2):keys.join(',')
EOF
"$ys" -T clj "$tmp/multi.ys" 2>/dev/null > "$tmp/multi.clj"
check "$(bb_run "$tmp/multi.clj")" 'DOC ONE
{"a":1}
z,a' 'multi-doc, str/ json/ aliases, omap key order'

# Same code gives same output under the ys runtime
check "$("$ys" "$tmp/multi.ys" 2>/dev/null)" 'DOC ONE
{"a":1}
z,a' 'ys runtime output matches'

# Round-trip: compiled output runs under ys -C (SCI ys.v0 stub)
check "$("$ys" -C "$tmp/multi.clj" 2>/dev/null)" 'DOC ONE
{"a":1}
z,a' 'ys -C round-trip'
