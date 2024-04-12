SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

YAMLSCRIPT_VERSION := 0.1.55

YS := $(wildcard ys)
LIBYAMLSCRIPT := $(firstword $(wildcard libyamlscript.*))

PREFIX ?= /usr/local

install:
ifneq (,$(YS))
	mkdir -p $(PREFIX)/bin
	cp -pP ys* $(PREFIX)/bin/
	@echo 'Installed $(PREFIX)/bin/$(YS) - version $(YAMLSCRIPT_VERSION)'
else ifneq (,$(LIBYAMLSCRIPT))
	mkdir -p $(PREFIX)/lib
	cp -pP libyamlscript* $(PREFIX)/lib/
	@echo 'Installed $(PREFIX)/lib/$(LIBYAMLSCRIPT) - version $(YAMLSCRIPT_VERSION)'
else
	$(error Weird! Nothing to install in this directory.)
endif
