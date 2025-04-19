# shellcheck disable=2034,2154

set -euo pipefail

set -x

root=$YAMLSCRIPT_ROOT
version=$YS_RELEASE_VERSION_NEW

main() (
  init
  clone
  update
  release
)

init() {
  repo_dir=$root/.git/tmp/yamlscript-$lang
  repo_url=git@github.com:yaml/yamlscript-$lang
  from_dir=$root/$lang
}

repo_url=git@github.com:yaml/yamlscript-$lang
from_dir=$root/$lang

clone() (
  rm -fr "$repo_dir"
  git clone "$repo_url" "$repo_dir"
)

release() (
  cd "$repo_dir" || exit
  git add -A .
  git commit -m "Release $YS_RELEASE_VERSION_NEW"
  git push
  git tag "v$YS_RELEASE_VERSION_NEW"
  git push --tags
)

true
