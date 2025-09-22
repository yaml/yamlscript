
set -euo pipefail

die() { printf '%s\n' "$@" >&2; exit 1; }

shopt -s inherit_errexit ||
  die "bash 4.4+ is required"

log() ( echo "* $*" >&2 )

if [[ $ys_binary != ys ]]; then
  [[ -f $ys_binary && -x $ys_binary ]] ||
    die "Bad value for ys_binary='$ys_binary'"
  PATH=$(dirname $ys_binary):$PATH
  export PATH
fi

if [[ -d $input_path ]]; then
  [[ -d $output_path ]] ||
    die "When input is a directory, --output must also be a directory"
fi

ys_jar_file=target/uberjar/main-file-build-standalone.jar
template=share/$build_type.clj

build_log=build.log
rm -f $build_log
if [[ ${YS_BUILD_DEBUG-} ]]; then
  build-log() (
    tee -a $build_log >&2
  )
  set -x
else
  build-log() {
    cat >> $build_log
  }
fi
