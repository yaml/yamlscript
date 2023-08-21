SHELL := bash

default:

.PHONY: test
test:
	julia --project=. test/runtests.jl

repl:
	julia --project=.

clean:
	$(RM) -r Manifest.toml
