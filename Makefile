SHELL := bash

ROOT := $(shell \
	cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

include $(ROOT)/common/vars.mk

DIRS := \
    core \
    libyamlscript \
    perl \
    perl-alien \
    python \
    raku \
    ruby \
    rust \
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

LYS_JAR_RELEASE := libyamlscript-$(YS_VERSION)-standalone.jar
YS_JAR_RELEASE := yamlscript.cli-$(YS_VERSION)-standalone.jar
LYS_JAR_PATH := libyamlscript/target/libyamlscript-$(YS_VERSION)-standalone.jar
YS_JAR_PATH := ys/target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

YS_RELEASE := $(RELEASE_YS_NAME).tar.xz
LYS_RELEASE := $(RELEASE_LYS_NAME).tar.xz

JAR_ASSETS := \
    $(LYS_JAR_RELEASE) \
    $(YS_JAR_RELEASE) \

RELEASE_ASSETS := \
    $(JAR_ASSETS) \

ifndef JAR_ONLY
RELEASE_ASSETS += \
    $(LYS_JAR_RELEASE) \
    $(YS_JAR_RELEASE)
endif

ifdef PREFIX
override PREFIX := $(abspath $(PREFIX))
endif

default:

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
	$(MAKE) -C ys test-all v=$v GRAALVM_O=b
test-%: %
	$(MAKE) -C $< test v=$v GRAALVM_O=b
test-unit:
	$(MAKE) -C core test v=$v
	$(MAKE) -C ys test v=$v

release: release-clean release-this

release-this: $(RELEASE_ASSETS)
	publish-release $(RELEASE_ASSETS)

release-build: release-build-ys release-build-libyamlscript

release-build-ys: $(YS_RELEASE)

release-build-libyamlscript: $(LYS_RELEASE)

release-clean:
	$(RM) -r libyamlscript/lib ys/bin ~/.m2/repository/yamlscript

jars: $(JAR_ASSETS)

$(YS_RELEASE): $(RELEASE_YS_NAME)
	mkdir -p $<
	cp -pP ys/bin/ys* $</
	cp common/install.mk $</Makefile
ifeq (true,$(IS_MACOS))
	$(TIME) tar -J -cf $@ $<
else
	$(TIME) tar -I'xz -0' -cf $@ $<
endif

$(LYS_RELEASE): $(RELEASE_LYS_NAME)
	mkdir -p $<
	cp -pP libyamlscript/lib/libyamlscript.$(SO)* $</
	cp common/install.mk $</Makefile
ifeq (true,$(IS_MACOS))
	$(TIME) tar -J -cf $@ $<
else
	$(TIME) tar -I'xz -0' -cf $@ $<
endif

$(RELEASE_YS_NAME): build-ys

$(RELEASE_LYS_NAME): build-libyamlscript

$(LYS_JAR_RELEASE): $(LYS_JAR_PATH)
	cp $< $@

$(YS_JAR_RELEASE): $(YS_JAR_PATH)
	cp $< $@

$(LYS_JAR_PATH):
	$(MAKE) -C libyamlscript jar

$(YS_JAR_PATH):
	$(MAKE) -C ys jar

delete-tag:
	-git tag --delete $(YS_VERSION)
	-git push --delete origin $(YS_VERSION)

bump:
	version-bump

$(CLEAN):
clean: $(CLEAN) release-clean
	$(RM) -r libyamlscript-0* ys-0* yamlscript.cli-*.jar
	$(RM) -r sample/advent/hearsay-rust/target/
	$(RM) NO-NAME
clean-%: %
	$(MAKE) -C $< clean

$(REALCLEAN):
realclean: clean $(REALCLEAN)
	$(MAKE) -C www $@
realclean-%: %
	$(MAKE) -C $< realclean

$(DISTCLEAN):
distclean: realclean $(DISTCLEAN)
	$(MAKE) -C www $@
	$(RM) -r bin/ lib/
distclean-%: %
	$(MAKE) -C $< distclean
	$(RM) -r .calva/ .clj-kondo/cache .lsp/

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
