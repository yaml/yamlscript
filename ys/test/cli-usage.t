#!/usr/bin/env bash

source test/init

VERSION=0.1.47


cmd='ys --version'
is "$($cmd)" "YAMLScript 0.1.47" "$cmd"


cmd='ys'
has "$($cmd)" \
  "ys - The YAMLScript (YS) Command Line Tool" \
  "$cmd"


cmd='ys -h'
has "$($cmd)" \
  "ys - The YAMLScript (YS) Command Line Tool" \
  "$cmd"


cmd='ys --help'
has "$($cmd)" \
  "ys - The YAMLScript (YS) Command Line Tool" \
  "$cmd"


is "$(ys -ce '=>: 1 + 2')" \
  "(+++ (+_ 1 2))" \
  "ys -ce '=>: 1 + 2'"


is "$(ys -pe '=>: 6 * 7')" \
  "42" \
  "ys -pe '=>: 6 * 7'"


is "$(ys -Cle '{:x 123}')" \
  '{"x":123}' \
  '--clojure works with -e'


cmd='ys -pl ...'
has "$($cmd)" \
  "Error: Options --print and --load are mutually exclusive." \
  '-p and -l mutually explusive'

cmd='ys -cp ...'
has "$($cmd)" \
  "Error: Options --print and --compile are mutually exclusive." \
  '-p and -l mutually explusive'


done-testing
