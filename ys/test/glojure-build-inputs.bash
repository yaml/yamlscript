#!/usr/bin/env bash

set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd -P)
temporary=$(mktemp -d)
runtime=
trap 'rm -rf "$temporary"; \
  [[ -z $runtime ]] || rm -f "$runtime/runtime/release_version_test.go"' EXIT

go=${YS_TEST_GO:?}
cli=${YS_TEST_CLI:?}
version=${YS_TEST_VERSION:?}
glj_version=${YS_TEST_GLJ_VERSION:?}
runtime_ref=${YS_TEST_RUNTIME_REF:?}

# Check the built executable, including the replacements used by its Go module.
"$go" version -m "$cli" > "$temporary/build-info"
grep -F "github.com/glojurelang/glojure" "$temporary/build-info" |
  grep -F "v$glj_version"
grep -F "$root/.cache/ys-v0-glj/$runtime_ref-" "$temporary/build-info"
[[ $("$cli" -pe VERSION) == "\"$version\"" ]]

# Simulate the source edits made by make release without changing this checkout.
mkdir -p "$temporary/source/yamlscript"
cp -R "$root/core/src/ys" "$temporary/source/"
cp "$root/core/src/yamlscript/regex.cljc" "$temporary/source/yamlscript/"
test_version=0.0.0-build-test
export YS_TEST_RELEASE_VERSION=$test_version
perl -0pi -e \
  's/\(def VERSION "[^"]+"\)/(def VERSION "$ENV{YS_TEST_RELEASE_VERSION}")/' \
  "$temporary/source/ys/v0.cljc"

prepare() {
  make --no-print-directory -s -C "$root/ys" glojure-build-info \
    YS_VERSION="$test_version" YS-V0-SOURCE="$temporary/source" \
    > "$temporary/runtime-info"
}

prepare
runtime=$(perl -ne 'print $1 if /^runtime-dir=(.*)/' "$temporary/runtime-info")
[[ -n $runtime ]]
grep -Fx "yamlscript=$test_version" "$runtime/.prepared"

cat > "$runtime/runtime/release_version_test.go" <<'GO'
package runtime

import (
    "os"
    "testing"

    "github.com/glojurelang/glojure/pkg/glj"
)

func TestYAMLScriptReleaseVersion(t *testing.T) {
    Load()
    got := glj.Var("clojure.core", "var-get").Invoke(glj.Var("ys.v0", "VERSION"))
    if want := os.Getenv("YS_TEST_RELEASE_VERSION"); got != want {
        t.Fatalf("embedded version = %v, want %s", got, want)
    }
}
GO
(
  cd "$runtime"
  "$go" test ./runtime -run '^TestYAMLScriptReleaseVersion$'
)

# An unchanged source tree must reuse its prepared runtime.
cp -p "$runtime/.prepared" "$temporary/prepared"
prepare
[[ ! $runtime/.prepared -nt $temporary/prepared ]]

# A source edit must invalidate that same cache entry.
export YS_TEST_RELEASE_VERSION=0.0.1-build-test
perl -0pi -e \
  's/\(def VERSION "[^"]+"\)/(def VERSION "$ENV{YS_TEST_RELEASE_VERSION}")/' \
  "$temporary/source/ys/v0.cljc"
prepare
(
  cd "$runtime"
  "$go" test ./runtime -run '^TestYAMLScriptReleaseVersion$'
)

echo 'Pinned compiler, release version, and runtime cache checks passed'
