#!/usr/bin/env bash
set -euo pipefail
format='' output='' html='' serve=''
while (($#)); do
  case $1 in
    --to) format=$2; shift 2 ;;
    --out) output=$2; shift 2 ;;
    --platform) shift 2 ;;
    --ext=html) html=1; shift ;;
    -Xserve*|-Xopen*) serve=1; shift ;;
    *) shift ;;
  esac
done
[[ $format && $output ]]
"$GLOAT_YS" --to=star input.ys >/dev/null
[[ ${YS_COMPILE_FAIL-} != 1 ]] || exit 9
mkdir -p "$(dirname "$output")"
if [[ $format == dir ]]; then
  mkdir -p "$output"
  echo 'package main' > "$output/main.go"
else
  echo "$format artifact" > "$output"
fi
if [[ $format == lib ]]; then
  echo 'int answer(void);' > "${output%.*}.h"
fi
if [[ $html ]]; then
  echo "fetch('result.js')" > "${output%.*}.html"
fi
if [[ $serve ]]; then
  html_output=${output%.js}.html
  if [[ ${output##*/} == index.js ]]; then
    bundle=${output%/*}
    url=http://localhost:8000/${bundle##*/}/index.html
  else
    url=http://localhost:8000/${html_output##*/}
  fi
  printf 'Now serving %s\n' "$url" >&2
fi
