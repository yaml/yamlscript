#!/usr/bin/env bash

set -euo pipefail

[[ ${YS_SH_DEBUG-} ]] && set -x

yamlscript_version=0.1.40

main() (
  setup "$@"

  "do-$command" "${arguments[@]}"
)

do-install() (
  export PREFIX=${PREFIX:-$(dirname "$bindir")}
  export LIB=1
  curl -sSL https://yamlscript.org/install | bash
)

do-upgrade() (
  export PREFIX=${PREFIX:-$(dirname "$bindir")}
  curl -sSL https://yamlscript.org/install | bash
)

do-compile-to-binary() (
  in_file=${1-}
  out_file=${2-}
  ys_version=${3-}
  [[ $in_file &&
     $out_file &&
     $ys_version &&
     ( $in_file == NO-NAME.ys || -f $in_file )
   ]] ||
    die "Usage: --compile-to-binary <in-file> <out-file> <ys-version>"
  [[ $in_file == *.ys ]] ||
    die "File '$in_file' must have .ys extension"

  in_base=$(cd -P -- "$(dirname -- "$in_file")" && pwd -P)
  in_name=$(basename -- "$in_file")
  in_path=$in_base/$in_name

  out_base=$(cd -P -- "$(dirname -- "$out_file")" && pwd -P)
  out_name=$(basename -- "$out_file")
  out_path=$out_base/${out_name%.ys}

  [[ $in_file == NO-NAME.ys ]] && in_file='-e'
  [[ $out_file == NO-NAME ]] && out_file='./NO-NAME'

  assert-lein
  assert-graalvm
  assert-yamlscript-core "$ys_version"

  dir=$(mktemp -d)
  (
    say "Compiling YAMLScript '$in_file' to '$out_file' executable"
    say "Setting up build env in '$dir'"
    cd "$dir" || exit
    write-makefile
    write-profile
    mkdir -p src
    write-program-clj "$in_path" "$in_file"
    say "This may take a few minutes..."
    make build 2>&1 |
      grep '^\[' |
      perl -pe 's/\.\.\..*\(/\t\t\(/g'
    cp program "$out_path"
    say "Compiled YAMLScript '$in_file' to '$out_file' executable"
  )
  rm -fr "$dir"
)

setup() {
  [[ ${1-} == --* ]] ||
    die "Usage: --<command> [args...]"

  command=${1#--}; shift
  arguments=("$@")

  bindir=$(cd -P "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)

  [[ $(command -v "do-$command") ]] ||
    die "Unknown command: --$command"

  # root=$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)

  lein_url=https://raw.githubusercontent.com/technomancy/
  lein_url+=leiningen/stable/bin/lein
  lein_path=/tmp/yamlscript/bin

  graalvm_subdir=''
  if [[ $OSTYPE == *linux* ]]; then
    if [[ $MACHTYPE == x86_64-*-linux* ]]; then
      graalvm_arch=linux-x64
    else
      die "Unsupported Linux architecture: $MACHTYPE"
    fi
  elif [[ $OSTYPE == *darwin* ]]; then
    graalvm_subdir=/Contents/Home
    if [[ $MACHTYPE == *arm*-apple-darwin* ]]; then
      graalvm_arch=macos-aarch64
    elif [[ $MACHTYPE == *x86_64-*-darwin* ]]; then
      graalvm_arch=macos-x64
    else
      die "Unsupported Mac architecture: $MACHTYPE"
    fi
  else
    die "Unsupported OS: $OSTYPE"
  fi

  graalvm_src=https://download.oracle.com/graalvm
  graalvm_ver=21
  graalvm_tar=graalvm-jdk-${graalvm_ver}_${graalvm_arch}_bin.tar.gz
  graalvm_dir_prefix=graalvm-jdk-${graalvm_ver}.
  graalvm_url=$graalvm_src/$graalvm_ver/latest/$graalvm_tar
  graalvm_path=/tmp/graalvm-oracle-$graalvm_ver
  graalvm_home=${graalvm_path}$graalvm_subdir
  graalvm_download=/tmp/$graalvm_tar
  graalvm_installed=$graalvm_home/release

  export JAVA_HOME=$graalvm_home
  export PATH=$lein_path:$graalvm_home/bin:$PATH
}

write-program-clj() (
  in_path=$1 in_file=$2

  if [[ $in_path == */NO-NAME.ys ]]; then
    program=$(
      "$YS_BIN" --compile --eval="$YS_CODE" |
        grep -Ev '^\(apply main ARGS\)'
    )
  else
    program=$(
      "$YS_BIN" --compile "$in_path" |
        grep -Ev '^\(apply main ARGS\)'
    )
  fi

  n=$'\n'
  [[ $program =~ \(defn[\ $n]+main[\ $n] ]] ||
    die "Could not find main function in '$in_file'"

  cat <<EOF > src/program.clj
(ns program (:gen-class) (:refer-clojure :exclude [print]))
(use 'clojure.core)
(use 'ys.std)
(def ^:dynamic ARGS [])
(def ^:dynamic ENV {})
;; ------------------------------------------------------------------------

$program

;; ------------------------------------------------------------------------
(defn -main [& args]
  (let [argv (vec
              (map
                #(if (re-matches #"\d+" %)
                  (parse-long %) %)
                  args))]
    (apply main argv)))
EOF
)

write-profile() (
  cat > project.clj <<EOF
(defproject program "ys-binary"
  :description "Compile a YAMLScript program to native binary executable"

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [org.babashka/sci "0.8.41"]
   [yamlscript/core "$yamlscript_version"]]

  :main ^:skip-aot program

  :target-path "target/%s"

  :prep-tasks
  [["compile"] ["javac"]]

  :java-source-paths ["src"]

  :profiles
  {:uberjar
   {:aot [program]
    :main program
    :global-vars
    {*assert* false
     *warn-on-reflection* true}
    :jvm-opts
    ["-Dclojure.compiler.direct-linking=true"
     "-Dclojure.spec.skip-macros=true"]}}

  :global-vars {*warn-on-reflection* true})
EOF
)

write-makefile() (
  cat > Makefile <<'EOF'
SHELL := bash

export PATH := /tmp/graalvm-oracle-21/bin:$(PATH)

JAR := target/uberjar/program-ys-binary-standalone.jar

OPTIONS := \
  -O1 \
  --native-image-info \
  --no-fallback \
  \
  --initialize-at-build-time \
  --enable-preview \
  \
  -H:+ReportExceptionStackTraces \
  -H:IncludeResources=SCI_VERSION \
  -H:Log=registerResource: \
  -J-Dclojure.spec.skip-macros=true \
  -J-Dclojure.compiler.direct-linking=true \
  -J-Xmx3g \

default:

build: program

program: $(JAR)
	time -p \
	native-image \
	    $(OPTIONS) \
	    -jar $< \
	    -o $@

$(JAR): src/program.clj Makefile project.clj
	lein uberjar

clean:
	$(RM) -r target reports
	$(RM) program
EOF
)

assert-yamlscript-core() (
  ys_version=$1
  ys_jar=$HOME/.m2/repository/yamlscript/core/$ys_version/core-$ys_version.jar
  if ! [[ -f $ys_jar ]]; then
    say "Installing YAMLScript core in '$ys_jar'"
    repo_path=/tmp/yamlscript/$ys_version
    assert-yamlscript-repo "$repo_path"
    (
      set -x
      make -C "$repo_path/core" install
    )
  fi
  [[ -f $ys_jar ]] ||
    die "Could not download YAMLScript core"
)

assert-yamlscript-repo() (
  repo_path=$1
  if ! [[ -d $repo_path ]]; then
    say "git cloning YAMLScript repo to '$repo_path'"
    mkdir -p "$repo_path"
    (
      set -x
      git clone --branch=$yamlscript_version --depth=1 --quiet \
        https://github.com/yaml/yamlscript \
        "$repo_path"
    )
  fi
)

assert-lein() {
  if ! [[ -f /tmp/yamlscript/bin/lein ]]; then
    say "Installing lein in '$lein_path/lein'"
    mkdir -p "$lein_path"
    (
      set -x
      curl -s "$lein_url" > "$lein_path/lein"
    )
  fi
  [[ -f $lein_path/lein ]] ||
    die "Could not download lein"
}

assert-graalvm() {
  if ! [[ -f $graalvm_installed ]]; then
    assert-graalvm-tarball
    say "Unpacking GraalVM in '$graalvm_path'"
    (
      set -x
      tar -xzf "$graalvm_download" -C /tmp
      mv "/tmp/$graalvm_dir_prefix"* "$graalvm_path"
    )
  fi
  [[ -f $graalvm_installed ]] ||
    die "Could not download GraalVM"
}

assert-graalvm-tarball() {
  if ! [[ -f $graalvm_download ]]; then
    say "Downloading GraalVM tarball in '$graalvm_download'"
    mkdir -p "$(dirname -- "$graalvm_download")"
    (
      set -x
      curl -s "$graalvm_url" > "$graalvm_download"
    )
  fi
  [[ -f $graalvm_download ]] ||
    die "Could not download GraalVM tarball"
}

say() (
  echo "* $*"
)

die() {
  printf '%s\n' "$@" >&2
  exit 1
}

[[ $0 != "${BASH_SOURCE[0]}" ]] || main "$@"
