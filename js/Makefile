SHELL := bash

COFFEE := $(shell find src -type f -name '*.coffee')
JS := $(COFFEE:%.coffee=%.js)
JS := $(JS:src/%=lib/%)

default:

build: $(JS)

lib/%.js: src/%.coffee
	@mkdir -p $(dir $@)
	coffee -c -p $< > $@

publish: build
	npm $@

clean:
	$(RM) package*
	$(RM) -r node_modules
