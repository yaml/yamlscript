include common/base.mk
include $(COMMON)/java.mk

BINDINGS := \
    clojure \
    crystal \
    go \
    java \
    julia \
    nodejs \
    perl \
    perl-alien \
    python \
    raku \
    ruby \
    rust \

DIRS := \
    core \
    libyamlscript \
    $(BINDINGS) \
    ys \

BUILD_DIRS := \
    libyamlscript \
    go \
    nodejs \
    python \
    ruby \
    rust \
    ys \

INSTALL_DIRS := \
    libyamlscript \
    ys \

BUILD := $(BUILD_DIRS:%=build-%)
BUILD_DOC := $(BINDINGS:%=build-doc-%)
INSTALL := $(INSTALL_DIRS:%=install-%)
TEST := $(DIRS:%=test-%)
TEST_BINDINGS := $(BINDINGS:%=test-%)
PUBLISH := $(DIRS:%=publish-%)
CLEAN := $(DIRS:%=clean-%)
REALCLEAN := $(DIRS:%=realclean-%)
DISTCLEAN := $(DIRS:%=distclean-%)

export HEAD := $(shell git rev-parse HEAD)

LYS_JAR_RELEASE := libyamlscript-$(YS_VERSION)-standalone.jar
YS_JAR_RELEASE := yamlscript.cli-$(YS_VERSION)-standalone.jar
LYS_JAR_PATH := libyamlscript/target/libyamlscript-$(YS_VERSION)-standalone.jar
YS_JAR_PATH := ys/target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

YS_RELEASE := $(RELEASE_YS_NAME).tar.xz
LYS_RELEASE := $(RELEASE_LYS_NAME).tar.xz

JAR_ASSETS := \
    $(LYS_JAR_RELEASE) \
    $(YS_JAR_RELEASE) \

ifndef JAR_ONLY
RELEASE_ASSETS := \
    $(LYS_RELEASE) \
    $(YS_RELEASE)
endif

RELEASE_ASSETS += \
    $(JAR_ASSETS) \

RELEASE_LOG := release-$n.log

ifdef PREFIX
override PREFIX := $(abspath $(PREFIX))
endif

ifdef v
export YS_RELEASE_VERBOSE := 1
endif
ifdef d
export YS_RELEASE_DRYRUN := 1
endif
ifdef l
export YS_RELEASE_LAST_STEP := $l
endif
ifdef n
export YS_RELEASE_VERSION_NEW := $n
endif
ifdef o
export YS_OLD_TAG := $o
export YS_RELEASE_VERSION_OLD := $o
endif

default::

env::
	@env | sort | less -FRX

chown::
	sudo chown -R $(USER):$(USER) .

ys-files:
	@( \
	  find . -name '*.ys' | cut -c3-; \
	  ag -asl --hidden '^#!/usr/bin/env ys-0$$' .; \
	) | grep -Ev '(^note/)' | \
	LC_ALL=C sort | uniq

nrepl nrepl-stop nrepl+:
	$(MAKE) -C core $@

$(BUILD):
build:: $(BUILD)
build-%: %
	$(MAKE) -C $< build

force:

$(BUILD_DOC):
build-doc: force $(BUILD_DOC)
	@:
build-doc-%: %
	$(MAKE) -C $< build-doc

$(INSTALL):
install: $(INSTALL)
install-%: % build-%
	-$(MAKE) -C $< install PREFIX=$(PREFIX)

$(TEST):
test: $(TEST)
	@echo
	@echo 'ALL TESTS PASSED!'
test-core:
	$(MAKE) -C core test v=$v
test-ys:
	$(MAKE) -C ys test v=$v GRAALVM_O=b
test-%: %
	$(MAKE) -C $< test v=$v GRAALVM_O=b
test-unit:
	$(MAKE) -C core test v=$v
	$(MAKE) -C ys test v=$v
test-bindings: $(TEST_BINDINGS)

serve publish:
	$(MAKE) -C www $@

ifdef s
release: release-check release-yamlscript
else
release: release-check realclean release-pull release-yamlscript
endif

release-check:
ifneq (main,$(shell git rev-parse --abbrev-ref HEAD))
	$(error Must be on branch 'main' to release)
endif
ifndef YS_GH_TOKEN
	$(error YS release requires YS_GH_TOKEN to be set)
endif
ifndef YS_GH_USER
	$(error YS release requires YS_GH_USER to be set)
endif
ifndef d
ifndef RELEASE_ID
ifndef YS_RELEASE_VERSION_OLD
	$(error 'make release' needs the 'o' variable set to the old version)
endif
ifndef YS_RELEASE_VERSION_NEW
	$(error 'make release' needs the 'n' variable set to the new version)
endif
ifeq (,$(shell which yarn))
	$(error 'make release' needs 'yarn' installed)
endif
endif
endif

release-pull:
ifndef d
	( \
	  set -ex; \
	  git pull --rebase; \
	  if [[ $$(git rev-parse HEAD) != $$HEAD ]]; then \
	    echo "Pulled new changes. Please re-run 'make release'."; \
	    exit 1; \
	  fi \
	)
endif

release-yamlscript: $(BUILD_BIN_YS)
ifneq (main, $(shell git rev-parse --abbrev-ref HEAD))
	$(error You must be on the 'main' branch to release)
endif
	@[[ $$YS_GH_USER ]] || { \
	  echo 'Please export YS_GH_USER'; exit 1; }
	@[[ $$YS_GH_TOKEN ]] || { \
	  echo 'Please export YS_GH_TOKEN'; exit 1; }
	(time $< $(ROOT)/util/release-yamlscript $o $n $s) 2>&1 | \
	  tee -a $(RELEASE_LOG)

release-assets: $(RELEASE_ASSETS)
	release-assets $^

release-build: release-build-ys release-build-libyamlscript

release-build-ys: $(YS_RELEASE)

release-build-libyamlscript: $(LYS_RELEASE)

jars: $(JAR_ASSETS)

$(YS_RELEASE): $(RELEASE_YS_NAME)
	mkdir -p $<
	cp -pPR ys/bin/ys* $</
	cp common/install.mk $</Makefile
ifeq (true,$(IS_MACOS))
	$(TIME) tar -J -cf $@ $<
else
	$(TIME) tar -I'xz -0' -cf $@ $<
endif

$(LYS_RELEASE): $(RELEASE_LYS_NAME)
	mkdir -p $<
	cp -pPR libyamlscript/lib/libyamlscript.$(SO)* $</
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

bump: $(BUILD_BIN_YS)
	$< $(ROOT)/util/version-bump

$(CLEAN):
clean:: $(CLEAN)
	$(RM) -r $(MAVEN_REPOSITORY)/yamlscript
	$(RM) -r $(MAVEN_REPOSITORY)/org/yamlscript
	$(RM) -r libyamlscript/lib ys/bin
	$(RM) -r libyamlscript-0* ys-0* yamlscript.cli-*.jar
	$(RM) -r sample/advent/hearsay-rust/target/
	$(RM) -r homebrew-yamlscript
	$(RM) NO-NAME release*.log
clean-%: %
	$(MAKE) -C $< clean

ifdef d
realclean::
else
$(REALCLEAN):
realclean:: clean $(REALCLEAN)
	$(MAKE) -C www $@
	$(RM) release-*
realclean-%: %
	$(MAKE) -C $< realclean
endif

$(DISTCLEAN):
distclean:: realclean $(DISTCLEAN)
	$(MAKE) -C www $@
	$(RM) -r bin/ lib/ website/
distclean-%: %
	$(MAKE) -C $< distclean
	$(RM) -r .calva/ .clj-kondo/.cache .lsp/

sysclean:: realclean
	$(RM) -r $(YS_TMP)
	$(RM) -r /tmp/yamlscript-* /tmp/ys-local
