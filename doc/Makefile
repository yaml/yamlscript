SHELL := bash

YS_FILES := $(shell echo *.ys)

MD_GEN_FILES := $(YS_FILES:%.ys=%.md)

default:

build: $(MD_GEN_FILES)

%.md: src/%.ys
	ys bin/gen-lib-doc.ys $< > $@

force:
	touch src/*.ys
