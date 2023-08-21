SHELL := bash

default:

build: _build

publish: build
	mix hex.publish

clean:
	$(RM) -r _build deps doc
	$(RM) yamlscript-*.tar

_build: deps
	mix hex.build

deps:
	mix deps.get
