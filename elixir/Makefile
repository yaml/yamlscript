SHELL := bash

default:

build: _build

publish: build
	mix hex.publish

clean:
	rm -fr _build deps doc
	rm -f yamlscript-*.tar

_build: deps
	mix hex.build

deps:
	mix deps.get
