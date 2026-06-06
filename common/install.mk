SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

YAMLSCRIPT_VERSION := 0.2.12

YS := $(wildcard ys)
LIBYS := $(firstword $(wildcard libys.*))

PREFIX ?= /usr/local

install:
ifneq (,$(YS))
	mkdir -p $(PREFIX)/bin
	cp -pP ys* $(PREFIX)/bin/
	@echo 'Installed $(PREFIX)/bin/$(YS)' \
		'- version $(YAMLSCRIPT_VERSION)'
else ifneq (,$(LIBYS))
	mkdir -p $(PREFIX)/lib
	cp -pP libys*.so* $(PREFIX)/lib/
	mkdir -p $(PREFIX)/include/libys-$(YAMLSCRIPT_VERSION)
	cp -pP *.h $(PREFIX)/include/libys-$(YAMLSCRIPT_VERSION)/
	@echo 'Installed $(PREFIX)/lib/$(LIBYS)' \
		'- version $(YAMLSCRIPT_VERSION)'
else
	$(error Weird! Nothing to install in this directory.)
endif
