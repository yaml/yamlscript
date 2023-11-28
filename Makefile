SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

include $(ROOT)/common/vars.mk

old ?= $(YS_VERSION)
new ?=

DIRS := \
    core \
    libyamlscript \
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
REALCLEAN := $(DIRS:%=realclean-%)
DISTCLEAN := $(DIRS:%=distclean-%)
DOCKER_BUILD := $(DIRS:%=docker-build-%)
DOCKER_TEST := $(DIRS:%=docker-test-%)
DOCKER_SHELL := $(DIRS:%=docker-shell-%)

ifdef PREFIX
override PREFIX := $(abspath $(PREFIX))
endif

default:

bump:
	version-bump $(old) $(new)

chown:
	sudo chown -R $(USER):$(USER) .

$(BUILD):
build: $(BUILD)
build-%: %
	$(MAKE) -C $< build

$(INSTALL):
install: $(INSTALL)
install-%: % build-%
	-$(MAKE) -C $< install PREFIX=$(PREFIX)

$(TEST):
test: $(TEST)
	@echo
	@echo 'ALL TESTS PASSED!'
test-ys:
	$(MAKE) -C ys test-all v=$v
test-%: %
	$(MAKE) -C $< test v=$v

$(CLEAN):
clean: $(CLEAN)
clean-%: %
	$(MAKE) -C $< clean

$(REALCLEAN):
realclean: clean $(REALCLEAN)
realclean-%: %
	$(MAKE) -C $< realclean

$(DISTCLEAN):
distclean: realclean $(DISTCLEAN)
distclean-%: %
	$(MAKE) -C $< distclean
	$(RM) -r .calva/ .clj-kondo/ .lsp/

sysclean: realclean
	$(RM) -r ~/.m2/ /tmp/graalvm* /tmp/yamlscript/

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
