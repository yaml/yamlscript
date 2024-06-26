SHELL := bash

ROOT := $(shell pwd)

YS_VERSION := 0.1.63

DIRS := \
    clojure \
    yamlscript \
    yamlscript-pm \

CLEAN := $(DIRS:%=clean-%)

C := --no-print-directory -C

YS_PATH := bin/ys-$(YS_VERSION)

default:
	@echo $(ROOT)

ys-install:
	$(MAKE) $C $(HOME)/src/yamlscript/ys install PREFIX=$(ROOT)

ys-install-quick:
	curl -s https://yamlscript.org/install | \
	  VERSION=$(YS_VERSION) PREFIX=$(ROOT) bash

clean: $(CLEAN)

clean-all: clean
	$(RM) -r bin/ys* lib

cols:
	tput $@

$(CLEAN):
	$(MAKE) $C $(@:clean-%=%) clean
