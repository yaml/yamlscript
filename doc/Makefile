SHELL := bash

MD_GEN_FILES := \
  stdlib.md \

export PATH := $(PWD)/util:$(PATH)


default:
	echo $(MD_GEN_FILES)

build: $(MD_GEN_FILES)

%.md: src/%.md
	markys $< > $@

force:
	touch src/*.md
