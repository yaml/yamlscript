# shellcheck disable=2034,2154

set -euo pipefail

set -x

root=$YAMLSCRIPT_ROOT
version=$YS_RELEASE_VERSION_NEW

main() (
  init
  clone
  update
  test
  release
)

git() (
  if [[ ${YS_RELEASE_DRY_RUN-} ]]; then
    echo "X - git $@"
  else
    command git "$@"
  fi
)

curl() (
  if [[ ${YS_RELEASE_DRY_RUN-} ]]; then
    echo "X - curl $@"
  else
    command curl "$@"
  fi
)

init() {
  repo_dir=$YS_TMPDIR/yamlscript-$lang
  repo_url=git@github.com:yaml/yamlscript-$lang
  from_dir=$root/$lang
}

repo_url=git@github.com:yaml/yamlscript-$lang
from_dir=$root/$lang

clone() (
  unset YS_RELEASE_DRY_RUN
  rm -fr "$repo_dir"
  git clone "$repo_url" "$repo_dir"
)

test() (:)

release() (
  cd "$repo_dir" || exit
  [[ ${YS_RELEASE_DRY_RUN-} ]] && set +x
  git add -A .
  git commit -m "Release $YS_RELEASE_VERSION_NEW"
  git push
  git tag "v$YS_RELEASE_VERSION_NEW"
  git push --tags
)

true
