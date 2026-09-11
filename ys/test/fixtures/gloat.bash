#!/usr/bin/env bash
set -euo pipefail
format='' output='' html=''
while (($#)); do
  case $1 in
    --to) format=$2; shift 2 ;;
    --out) output=$2; shift 2 ;;
    --platform) shift 2 ;;
    --ext=html) html=1; shift ;;
    *) shift ;;
  esac
done
[[ $format && $output ]]
"$GLOAT_YS" --to=star input.ys >/dev/null
[[ ${YS_COMPILE_FAIL-} != 1 ]] || exit 9
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
