SHELL := bash

default:

.PHONY: test
test:
	julia --project=. test/runtests.jl

repl:
	julia --project=.

clean:
	rm -fr Manifest.toml
