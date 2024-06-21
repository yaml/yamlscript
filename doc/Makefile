SHELL := bash

YS_FILES := $(shell echo *.ys)

MD_GEN_FILES := $(YS_FILES:%.ys=%.md)

export PATH := $(PWD)/doc/bin:$(PATH)


default:

build: $(MD_GEN_FILES)

%.md: src/%.ys
	markdowny $< > $@

force:
	touch src/*.ys
