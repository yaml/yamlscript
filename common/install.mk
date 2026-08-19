SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

YAMLSCRIPT_VERSION := 0.2.30

YS-FILES := $(filter-out %-build-report.html,\
	$(wildcard ys ys.exe ys-[0-9]* ys-sh-[0-9]*))
YS := $(firstword $(YS-FILES))
LIBYS := $(firstword $(wildcard libys.so* libys.dylib* libys.dll))
LIBYS-FILES := $(wildcard libys.so* libys.dylib* libys.dll)

PREFIX ?= /usr/local

# Set M2=0 to skip installing the bundled ys.v0 jars into ~/.m2
M2 ?= 1

install:
ifneq (,$(YS))
	mkdir -p $(PREFIX)/bin
	cp -pP $(YS-FILES) $(PREFIX)/bin/
	@echo 'Installed $(PREFIX)/bin/$(YS)' \
		'- version $(YAMLSCRIPT_VERSION)'
ifneq (,$(wildcard m2/repository))
ifneq (0,$(M2))
	@if [[ $$(id -u) == 0 ]]; then \
	  echo "Not installing the ys.v0 jars into ~/.m2 (running as root)."; \
	  echo "Run 'ys-sh-$(YAMLSCRIPT_VERSION) --install-m2' as a normal"; \
	  echo "user to enable java free 'ys -T bb' scripts."; \
	else \
	  mkdir -p $$HOME/.m2/repository; \
	  cp -pR m2/repository/. $$HOME/.m2/repository/; \
	  jar=$$HOME/.m2/repository/org/yamlscript/ys.v0; \
	  jar=$$jar/$(YAMLSCRIPT_VERSION)/ys.v0-$(YAMLSCRIPT_VERSION).jar; \
	  if [[ ! -e $$jar.d ]] && command -v unzip >/dev/null; then \
	    unzip -o -q $$jar -d $$jar.d; \
	  fi; \
	  echo "Installed the ys.v0 jars into ~/.m2/repository"; \
	fi
endif
endif
else ifneq (,$(LIBYS))
	mkdir -p $(PREFIX)/lib
	cp -pP $(LIBYS-FILES) $(PREFIX)/lib/
	mkdir -p $(PREFIX)/include/libys-$(YAMLSCRIPT_VERSION)
	cp -pP *.h $(PREFIX)/include/libys-$(YAMLSCRIPT_VERSION)/
	@echo 'Installed $(PREFIX)/lib/$(LIBYS)' \
		'- version $(YAMLSCRIPT_VERSION)'
else
	$(error Weird! Nothing to install in this directory.)
endif
