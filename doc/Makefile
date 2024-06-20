SHELL := bash

YS_FILES := $(shell echo *.ys)

MD_GEN_FILES := $(YS_FILES:%.ys=%.md)

default:

docs: $(MD_GEN_FILES)

%.md: %.ys Makefile
	ys bin/gen-lib-doc.ys $< > $@
