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
    libys \
    $(BINDINGS) \
    ys \

BUILD-DIRS := \
    libys \
    go \
    nodejs \
    python \
    ruby \
    rust \
    ys \

INSTALL-DIRS := \
    libys \
    ys \

BUILD := $(BUILD-DIRS:%=build-%)
BUILD-DOC := $(BINDINGS:%=build-doc-%)
INSTALL := $(INSTALL-DIRS:%=install-%)
TEST := $(DIRS:%=test-%)
TEST-BINDINGS := $(BINDINGS:%=test-%)
PUBLISH := $(DIRS:%=publish-%)
CLEAN := $(DIRS:%=clean-%)
REALCLEAN := $(DIRS:%=realclean-%)
DISTCLEAN := $(DIRS:%=distclean-%)

export HEAD := $(shell git rev-parse HEAD)

LYS-JAR-RELEASE := libys-$(YS_VERSION)-standalone.jar
YS-JAR-RELEASE := yamlscript.cli-$(YS_VERSION)-standalone.jar
LYS-JAR-PATH := libys/target/libys-$(YS_VERSION)-standalone.jar
YS-JAR-PATH := ys/target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

YS-RELEASE := $(RELEASE-YS-NAME).tar.xz
LYS-RELEASE := $(RELEASE-LYS-NAME).tar.xz

JAR-ASSETS := \
    $(LYS-JAR-RELEASE) \
    $(YS-JAR-RELEASE) \

ifndef JAR_ONLY
RELEASE-ASSETS := \
    $(LYS-RELEASE) \
    $(YS-RELEASE)
endif

RELEASE-ASSETS += \
    $(JAR-ASSETS) \

RELEASE-LOG := release-$n.log

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

$(BUILD-DOC):
build-doc: force $(BUILD-DOC)
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
	$(MAKE) -C ys test v=$v GRAALVM-O=b
test-%: %
	$(MAKE) -C $< test v=$v GRAALVM-O=b
test-unit:
	$(MAKE) -C core test v=$v
	$(MAKE) -C ys test v=$v
test-bindings: $(TEST-BINDINGS)

serve publish:
	$(MAKE) -C www $@

ifdef s
release: release-check release-yamlscript
else
release: release-check realclean release-pull release-yamlscript
endif

release-check:
ifndef YS_RELEASE_NO_CHECK
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
ifndef RELEASE-ID
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

release-yamlscript: $(YS)
ifndef YS_RELEASE_NO_CHECK
ifneq (main, $(shell git rev-parse --abbrev-ref HEAD))
	$(error You must be on the 'main' branch to release)
endif
	@[[ $$YS_GH_USER ]] || { \
	  echo 'Please export YS_GH_USER'; exit 1; }
	@[[ $$YS_GH_TOKEN ]] || { \
	  echo 'Please export YS_GH_TOKEN'; exit 1; }
endif
	(time $< $(ROOT)/util/release-yamlscript $o $n $s) 2>&1 | \
	  tee -a $(RELEASE-LOG)

release-assets: $(RELEASE-ASSETS)
	release-assets $^

release-build: release-build-ys release-build-libys

release-build-ys: $(YS-RELEASE)

release-build-libys: $(LYS-RELEASE)

jars: $(JAR-ASSETS)

$(YS-RELEASE): $(RELEASE-YS-NAME)
	mkdir -p $<
	cp -pPR ys/bin/ys* $</
	cp common/install.mk $</Makefile
ifeq (true,$(IS-MACOS))
	$(TIME) tar -J -cf $@ $<
else
	$(TIME) tar -I'xz -0' -cf $@ $<
endif

$(LYS-RELEASE): $(RELEASE-LYS-NAME)
	mkdir -p $<
	cp -pPR libys/lib/libys.$(SO)* $</
	cp common/install.mk $</Makefile
ifeq (true,$(IS-MACOS))
	$(TIME) tar -J -cf $@ $<
else
	$(TIME) tar -I'xz -0' -cf $@ $<
endif

$(RELEASE-YS-NAME): build-ys

$(RELEASE-LYS-NAME): build-libys

$(LYS-JAR-RELEASE): $(LYS-JAR-PATH)
	cp $< $@

$(YS-JAR-RELEASE): $(YS-JAR-PATH)
	cp $< $@

$(LYS-JAR-PATH):
	$(MAKE) -C libys jar

$(YS-JAR-PATH):
	$(MAKE) -C ys jar

delete-tag:
	-git tag --delete $(YS_VERSION)
	-git push --delete origin $(YS_VERSION)

bump: $(YS)
	$< $(ROOT)/util/version-bump

$(CLEAN):
clean:: $(CLEAN)
	$(RM) -r $(MAVEN-REPOSITORY)/yamlscript
	$(RM) -r $(MAVEN-REPOSITORY)/org/yamlscript
	$(RM) -r libys/lib ys/bin
	$(RM) -r libys-0* ys-0* yamlscript.cli-*.jar
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
	$(RM) -r $(ROOT)/.cache/
