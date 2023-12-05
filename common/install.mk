SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

PREFIX ?= /usr/local

install:
ifneq (,$(wildcard ys*))
	mkdir -p $(PREFIX)/bin
	cp -pP ys* $(PREFIX)/bin/
else ifneq (,$(wildcard libyamlscript*))
	mkdir -p $(PREFIX)/lib
	cp -pP libyamlscript* $(PREFIX)/lib/
else
	$(error Weird! Nothing to install in this directory.)
endif
