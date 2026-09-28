#!/usr/bin/env bash

# End-to-end tests: `ys -Tclj+` output runs under each supported dialect.

set -u

root=$(cd "$(dirname "$0")/../.." && pwd -P)
ys=${YS_BIN:-$root/util/ysj}
jolt=${JOLT_BIN:-$root/repos/jolt/target/release/jolt}
gobb=${GOBB_BIN:-$root/repos/gobb/bin/gobb}
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

bail() {
  echo "Bail out! $*"
  exit 1
}

if [[ -n ${GLJ_BIN:-} ]]; then
  glj=$GLJ_BIN
else
  shopt -s nullglob
  glj_bins=("$root"/repos/glojure/bin/*/glj)
  shopt -u nullglob
  [[ ${#glj_bins[@]} == 1 ]] ||
    bail 'set GLJ_BIN to the one Glojure executable to test'
  glj=${glj_bins[0]}
fi

for spec in "Jolt:$jolt" "Glojure:$glj" "Gobb:$gobb"; do
  name=${spec%%:*}
  executable=${spec#*:}
  [[ -x $executable ]] || bail "$name executable not found: $executable"
done

if ! make -s -C "$root/v0" install HOME="$tmp/home" >/dev/null; then
  bail 'could not stage ys.v0 in the isolated Maven repository'
fi
m2=$tmp/home/.m2/repository

# shellcheck disable=SC2016  # YAMLScript interpolation is literal input.
if ! "$ys" -Tclj+ -e \
  'defn main(): say("Hi $(ENV.USER:uc1)!")' \
  >"$tmp/program.clj" 2>"$tmp/compile.err"; then
  bail "YAMLScript compilation failed: $(<"$tmp/compile.err")"
fi

count=0
check() {
  local got=$1 want=$2 name=$3
  got=${got//$'\r'/}
  count=$((count+1))
  if [[ $got == "$want" ]]; then
    echo "ok $count - $name"
  else
    echo "not ok $count - $name"
    echo "# got:  '$got'"
    echo "# want: '$want'"
  fi
}

run_dialect() {
  local name=$1 executable=$2 input=${3:--} output status
  shift 3
  local error=$tmp/${name,,}.err
  output=$(
    cd "$tmp" || exit 1
    env \
      HOME="$tmp/home" \
      USER=codex \
      GRENADINE_MAVEN_REPOSITORY="$m2" \
      GLOJURE_MAVEN_REPOSITORY="$m2" \
      JOLT_MAVEN_REPOSITORY="$m2" \
      GOBB_MAVEN_REPOSITORY="$m2" \
      JOLT_QUIET=1 \
      "$executable" "$@" "$input" \
        <"$tmp/program.clj" 2>"$error"
  )
  status=$?
  check "$status" 0 "$name exits successfully"
  check "$output" 'Hi Codex!' "$name output"
  check "$(<"$error")" '' "$name has no diagnostics"
}

echo '1..15'
run_dialect Jolt "$jolt" -
run_dialect Glojure "$glj" -
run_dialect Gobb "$gobb" -
run_dialect Gobb-dev-stdin "$gobb" /dev/stdin
run_dialect YAMLScript "$ys" - -C
