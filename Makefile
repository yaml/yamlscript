include common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/docker.mk

include $(MAKES)/gh.mk
# Languages whose CLIs 'secrets-update' uses to authenticate. Each entry
# installs that toolchain locally and puts it on PATH (never system).
# Must track the services that declare a 'login'/'fetch' in
# util/yamlscript-secrets (npm -> node; fez -> raku).
SECRETS-LANGS := node raku
SHELL-LANGS += $(SECRETS-LANGS)
include $(SHELL-LANGS:%=$(MAKES)/%.mk)
include $(MAKES)/shell.mk

FEZ := $(RAKU-SITE-BIN)/fez
SECRETS-TOOLS := $(NODE) $(FEZ)

BINDINGS := \
    clojure \
    crystal \
    csharp \
    go \
    haskell \
    java \
    julia \
    lua \
    nodejs \
    perl \
    perl-alien \
    php \
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
    haskell \
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

ifdef YS_RELEASE_SKIP_HASKELL_TESTS
TEST := $(filter-out test-haskell,$(TEST))
TEST-BINDINGS := $(filter-out test-haskell,$(TEST-BINDINGS))
endif

export HEAD := $(shell git rev-parse HEAD)

LYS-JAR-RELEASE := libys-$(YS_VERSION)-standalone.jar
YS-JAR-RELEASE := yamlscript.cli-$(YS_VERSION)-standalone.jar
LYS-JAR-PATH := libys/target/libys-$(YS_VERSION)-standalone.jar
YS-JAR-PATH := \
    ys/target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

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
RELEASE-SECRETS := $(wildcard $(HOME)/.yamlscript-secrets.yaml)
RELEASE-AUTH := \
    $(strip $(GH_TOKEN)$(GITHUB_TOKEN)$(RELEASE-SECRETS))

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
test:
	$(TIME) $(MAKE) test-all
test-all: $(TEST)
	@echo
	@echo 'ALL TESTS PASSED!'
test-core:
	@echo
	@echo "=== Testing 'core' ==="
	@echo
	$(TIME) $(MAKE) -C core test v=$v
test-ys:
	@echo
	@echo "=== Testing 'ys' ==="
	@echo
	$(TIME) $(MAKE) -C ys test v=$v GRAALVM-O=b
test-%: %
	@echo
	@echo "=== Testing '$<' ==="
	@echo
	$(TIME) $(MAKE) -C $< test v=$v GRAALVM-O=b
test-unit:
	$(TIME) $(MAKE) -C core test v=$v
	$(TIME) $(MAKE) -C ys test v=$v
test-bindings: $(TEST-BINDINGS)

serve publish:
	$(MAKE) -C www $@

ifneq (,$(or $s,$(YS_RELEASE_ID),$(YS_RELEASE_NO_CHECK)))
release: _release-yamlscript
else
release: release-check realclean release-pull _release-yamlscript
endif

release-check:
ifndef YS_RELEASE_NO_CHECK
ifneq (main,$(shell git rev-parse --abbrev-ref HEAD))
	$(error Must be on branch 'main' to release)
endif
ifeq (,$(RELEASE-AUTH))
	$(error YS release requires GH_TOKEN or $(SECRETS) file)
endif
endif
ifndef d
ifndef RELEASE-ID
ifndef YS_RELEASE_VERSION_OLD
	$(error 'make release' needs the 'o' variable set to the old version)
endif
ifndef YS_RELEASE_VERSION_NEW
	$(error 'make release' needs the 'n' variable set to the new version)
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

ifdef IS-LINUX
ifdef IS-INTEL
docker-build := YS_BUILD_IN_DOCKER=1
endif
endif

_release-yamlscript: $(YS) $(GH)
	($(TIME) $(docker-build) \
	  ys $(ROOT)/util/release-yamlscript release $o $n $s) 2>&1 | \
	  tee -a $(RELEASE-LOG)

release-assets: $(RELEASE-ASSETS)
	release-assets $^

release-build: release-build-ys release-build-libys

release-build-ys: $(YS-RELEASE)

release-build-libys: $(LYS-RELEASE)

#------------------------------------------------------------------------------
# Interactive Release Workflow - Individual Step Targets
#------------------------------------------------------------------------------

# Show all release steps
release-list: $(YS)
	$(YS) $(ROOT)/util/release-yamlscript list

# Step 1: Sanity checks
release-sanity-check: $(YS)
ifndef o
	$(error 'make release-sanity-check' requires o=OLD_VERSION n=NEW_VERSION)
endif
ifndef n
	$(error 'make release-sanity-check' requires o=OLD_VERSION n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript sanity-check $(o) $(n)

# Step 2: Credential updates
release-secrets-update: $(YS) $(GH) $(SECRETS-TOOLS)
	$(YS) $(ROOT)/util/release-yamlscript secrets-update

# Step 3: Credential status
release-secrets-list: $(YS)
	$(YS) $(ROOT)/util/release-yamlscript secrets-list

# Step 4: Publish credentials
release-secrets-publish: $(YS) $(GH)
	$(YS) $(ROOT)/util/release-yamlscript secrets-publish

# Step 5: Version bump
release-version-bump: $(YS)
	$(YS) $(ROOT)/util/release-yamlscript version-bump

# Step 6: Changelog
release-changelog: $(YS)
ifndef o
	$(error 'make release-changelog' requires o=OLD_VERSION n=NEW_VERSION)
endif
ifndef n
	$(error 'make release-changelog' requires o=OLD_VERSION n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript changelog $(o) $(n)

# Step 4 (legacy): Test (kept for backwards compatibility)
release-test:
	@echo "Note: Tests now run automatically in GitHub Actions"
	@echo "Use 'gh workflow run test-all.yaml' to trigger manually"

# Step 7: Binding changelogs
release-binding-changelogs: $(YS)
	$(YS) $(ROOT)/util/release-yamlscript binding-changelogs

# Step 8: Commit
release-commit: $(YS)
ifndef n
	$(error 'make release-commit' requires n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript commit $(n)

# Step 9: Tag
release-tag: $(YS)
ifndef n
	$(error 'make release-tag' requires n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript tag $(n)

# Step 10: Push
release-push: $(YS)
ifndef n
	$(error 'make release-push' requires n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript push $(n)

# Step 11: Trigger GitHub Actions
release-build-github: $(YS)
ifndef n
	$(error 'make release-build-github' requires n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript build-github $(n)

# Retry a failed unpublished release by moving the existing tag to HEAD and
# redispatching the GitHub release workflow.
release-retry: $(YS) $(GH)
ifndef n
	$(error 'make release-retry' requires n=NEW_VERSION)
endif
	@if gh release view $(n) --repo yaml/yamlscript >/dev/null 2>&1; then \
	  echo "Error: GitHub release $(n) already exists."; \
	  echo "Refusing to retag a release that may be published."; \
	  exit 1; \
	fi
	git push origin main
	git tag -f $(n) HEAD
	git push -f origin $(n)
	$(MAKE) release-build-github n=$(n)

# Step 12: Release bindings
release-bindings: $(if $(YS_RELEASE_CI),,$(YS))
ifndef n
	$(error 'make release-bindings' requires n=NEW_VERSION)
endif
	$(YS) $(ROOT)/util/release-yamlscript bindings $(n)

# Step 13: Publish website
release-website: $(YS)
	$(YS) $(ROOT)/util/release-yamlscript website

#------------------------------------------------------------------------------
# Release Credentials Management
#------------------------------------------------------------------------------

# Tools the credential-login CLIs need, installed locally (never
# system). Grows as services gain a 'login'/'fetch' in
# util/yamlscript-secrets. node provides `npm login`; fez (installed
# via zef on the local rakudo) provides `fez login`.
$(FEZ): $(RAKU)
	zef install fez
	touch $@

# Refresh due publishing credentials (or a forced list like
# SECRETS=npm,clojars)
secrets-update: $(YS) $(GH) $(SECRETS-TOOLS)
	$(YS) $(ROOT)/util/yamlscript-secrets --update=$(or $(SECRETS),all)

# List credential status: present (masked) and rotation due date
secrets-list: $(YS)
	$(YS) $(ROOT)/util/yamlscript-secrets --list

# Publish all stored credentials as GitHub repo secrets
secrets-publish: $(YS) $(GH)
	$(YS) $(ROOT)/util/yamlscript-secrets --publish

# Prepare a release: refresh credentials, bump versions, and update
# changelogs. Usage: make release-prep o=OLD n=NEW
release-prep: $(YS) $(GH)
ifndef o
	$(error 'make release-prep' requires o=OLD_VERSION n=NEW_VERSION)
endif
ifndef n
	$(error 'make release-prep' requires o=OLD_VERSION n=NEW_VERSION)
endif
	$(MAKE) secrets-update
	$(MAKE) secrets-list
	$(MAKE) secrets-publish
	$(MAKE) release-version-bump
	$(MAKE) release-changelog o=$(o) n=$(n)
	$(MAKE) release-binding-changelogs

#------------------------------------------------------------------------------

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
	cp -pPR libys/lib/libys*.$(SO)* $</
	cp -pPR libys/lib/*.h $</
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
	$(RM) NO-NAME release*.log release-changes.txt release-id.txt
clean-%: %
	$(MAKE) -C $< clean

ifdef d
realclean::
else
$(REALCLEAN):
realclean:: clean $(REALCLEAN)
	$(MAKE) docker-kill
	$(MAKE) -C www $@
	$(RM) $(DOCKER-FILE)
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
	$(RM) -r $(ROOT)/.clj-kondo/.cache/
