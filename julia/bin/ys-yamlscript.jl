#!/bin/bash

# https://docs.julialang.org/en/v1/manual/faq/#How-do-I-pass-options-to-julia-using-#!/usr/bin/env?
#=
exec julia --project=. --color=yes --startup-file=no -e 'include(popfirst!(ARGS))' \
    "${BASH_SOURCE[0]}" "$@"
=#

using YAMLScript

file = popfirst!(ARGS)

YAMLScript.new().fromFile(file).run(ARGS)

# vim: ft=julia:
