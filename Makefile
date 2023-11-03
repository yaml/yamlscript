SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

include $(ROOT)/common/vars.mk

DIRS := \
    clojure \
    libyamlscript \
    perl \
    python \
    ys \

BUILD_DIRS := \
		libyamlscript \
		ys \

BUILD := $(BUILD_DIRS:%=build-%)
INSTALL := $(BUILD_DIRS:%=install-%)
TEST := $(DIRS:%=test-%)
PUBLISH := $(DIRS:%=publish-%)
CLEAN := $(DIRS:%=clean-%)
DISTCLEAN := $(DIRS:%=distclean-%)
DOCKER_BUILD := $(DIRS:%=docker-build-%)
DOCKER_TEST := $(DIRS:%=docker-test-%)
DOCKER_SHELL := $(DIRS:%=docker-shell-%)

default:

chown:
	sudo chown -R $(USER):$(USER) .

$(BUILD):
build: $(BUILD)
build-%: %
	$(MAKE) -C $< build

install-local: install/bin/ys install/lib/libyamlscript.$(SO)
install/bin/ys: ys/ys install/bin
	cp $< $@
install/lib/libyamlscript.$(SO): libyamlscript/lib/libyamlscript.$(SO) install/lib
	cp $< $@
install/bin install/lib:
	mkdir -p $@

$(INSTALL):
install: $(INSTALL)
install-%: % build-%
	-$(MAKE) -C $< install

$(TEST):
test: $(TEST)
test-%: %
	$(MAKE) -C $< test v=$v

$(CLEAN):
clean: $(CLEAN)
	$(RM) -r install/
clean-%: %
	$(MAKE) -C $< clean

clean-install:
	$(RM) -r install/

clean-all: $(DISTCLEAN)

$(DISTCLEAN):
distclean: clean $(DISTCLEAN)
distclean-%: %
	$(MAKE) -C $< distclean
	$(RM) -r .calva/ .clj-kondo/ .lsp/

$(DOCKER_BUILD):
docker-build: $(DOCKER_BUILD)
docker-build-%: %
	$(MAKE) -C $< docker-build

$(DOCKER_TEST):
docker-test: $(DOCKER_TEST)
docker-test-%: %
	$(MAKE) -C $< docker-test v=$v

$(DOCKER_SHELL):
docker-shell-%: %
	$(MAKE) -C $< docker-shell v=$v
