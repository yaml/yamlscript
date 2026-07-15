# shellcheck disable=2034,2154

set -euo pipefail

[[ ${YS_RELEASE_VERBOSE-} ]] && set -x

root=$YAMLSCRIPT_ROOT
version=${YS_RELEASE_VERSION_NEW:-${n:-}}

[[ $version ]] || {
  echo 'Error: YS_RELEASE_VERSION_NEW or n must be set' >&2
  exit 1
}

main() (
  init
  clone
  update
  if [[ ! ${YS_RELEASE_CI-} ]]; then
    test
  fi
  release
)

git() (
  if [[ ${YS_RELEASE_DRYRUN-} || ${YS_RELEASE_DRY_RUN-} ]]; then
    echo "X - git $@"
  else
    command git "$@"
  fi
)

curl() (
  if [[ ${YS_RELEASE_DRYRUN-} || ${YS_RELEASE_DRY_RUN-} ]]; then
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

clone() (
  rm -fr "$repo_dir"
  git clone "$repo_url" "$repo_dir"
  if [[ ${YS_RELEASE_DRYRUN-} || ${YS_RELEASE_DRY_RUN-} ]]; then
    mkdir -p "$repo_dir"
  fi
)

test() (:)

release() (
  cd "$repo_dir" || exit
  git add -A .

  if git diff --cached --quiet; then
    echo "No changes for yamlscript-$lang"
  else
    git commit -m "Release $version"
    git push origin HEAD
  fi

  if git rev-parse "v$version" >/dev/null 2>&1; then
    git tag -d "v$version"
  fi
  git tag "v$version"
  git push origin "v$version"

  if declare -F after_release >/dev/null; then
    after_release
  fi
)

true
