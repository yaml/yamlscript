SHELL := bash

DIRS := \
    clojure \
    libyamlscript \
    perl \
    python \

TEST := $(DIRS:%=test-%)
PUBLISH := $(DIRS:%=publish-%)
CLEAN := $(DIRS:%=clean-%)
DISTCLEAN := $(DIRS:%=distclean-%)

default:

build: libyamlscript
	$(MAKE) -C $< build

test: $(TEST)
test-%: %
	$(MAKE) -C $< test

clean: $(CLEAN)
clean-%: %
	$(MAKE) -C $< clean

distclean: $(DISTCLEAN)
distclean-%: %
	$(MAKE) -C $< distclean
