#------------------------------------------------------------------------------
# Set machine specific variables:
#------------------------------------------------------------------------------
ifneq (,$(findstring linux,$(ostype)))
  GRAALVM_SUBDIR :=

  ifeq (x86_64-pc-linux-gnu,$(machtype))
    GRAALVM_ARCH := linux-x64

  else
    $(error Unsupported Linux MACHTYPE: $machtype)
  endif

else ifneq (,$(findstring darwin,$(ostype)))
  GRAALVM_SUBDIR := /Contents/Home

  ifneq (,$(findstring arm64-apple-darwin,$(machtype)))
    GRAALVM_ARCH := macos-aarch64

  else ifneq (,$(findstring x86_64-apple-darwin,$(machtype)))
    GRAALVM_ARCH := macos-x64

  else
    $(error Unsupported MacOS MACHTYPE: $machtype)
  endif

else
  $(error Unsupported OSTYPE: $ostype)
endif

#------------------------------------------------------------------------------
# Set GRAALVM variables:
#------------------------------------------------------------------------------

### For Orable GraalVM No-Fee #################################################
ifndef GRAALVM_CE
GRAALVM_SRC := https://download.oracle.com/graalvm
GRAALVM_VER ?= 21
GRAALVM_TAR := graalvm-jdk-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/latest/$(GRAALVM_TAR)
GRAALVM_PATH ?= /tmp/graalvm-oracle-$(GRAALVM_VER)

### For GraalVM CE (Community Edition) ########################################
else
GRAALVM_SRC := https://github.com/graalvm/graalvm-ce-builds/releases/download
GRAALVM_VER ?= 21
  ifeq (21,$(GRAALVM_VER))
    override GRAALVM_VER := jdk-21.0.0
  endif
  ifeq (17,$(GRAALVM_VER))
    override GRAALVM_VER := jdk-17.0.8
  endif
GRAALVM_TAR := graalvm-community-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/$(GRAALVM_TAR)
GRAALVM_PATH ?= /tmp/graalvm-ce-$(GRAALVM_VER)
endif

GRAALVM_HOME := $(GRAALVM_PATH)$(GRAALVM_SUBDIR)
GRAALVM_DOWNLOAD := /tmp/$(GRAALVM_TAR)

GRAALVM_O ?= 1

export JAVA_HOME := $(GRAALVM_HOME)
export PATH := $(GRAALVM_HOME)/bin:$(PATH)

YAMLSCRIPT_CORE_INSTALLED := \
  $(HOME)/.m2/repository/yamlscript/core/maven-metadata-local.xml
YAMLSCRIPT_CORE_SRC := ../clojure/src/yamlscript/*

ifdef w
  export WARN_ON_REFLECTION := 1
endif

LEIN_COMMANDS := \
  check \
  classpath \
  compile \
  deps \
  deploy \
  run \
  show-profiles \

define HUMANE_TEST_INIT
(do \
  (require 'pjstadig.humane-test-output) \
  (pjstadig.humane-test-output/activate!) \
  (require 'yamlscript.test-runner))
endef

LEIN_REPL_OPTIONS := \
  update-in :dependencies conj '[nrepl,"1.0.0"]' -- \
  update-in :nrepl-middleware conj '["cider.nrepl/cider-middleware"]' -- \
  update-in :plugins conj '[cider/cider-nrepl,"0.28.5"]' -- \
  update-in :repl-options conj '["cider.nrepl/cider-middleware"]' -- \

#------------------------------------------------------------------------------
# Common Clojure Targets
#------------------------------------------------------------------------------

clean:: nrepl-stop
	$(RM) pom.xml
	$(RM) -r .lein-*
	$(RM) -r reports/ target/

distclean::
	$(RM) -r .calva/ .clj-kondo/ .cpcache/ .lsp/ .vscode/ .portal/

# Leiningen targets
$(LEIN_COMMANDS)::
	lein $@

deps-graph::
	lein deps :tree

# Build/GraalVM targets
force:
	$(RM) $(YAMLSCRIPT_CORE_INSTALLED)

$(YAMLSCRIPT_CORE_INSTALLED): $(YAMLSCRIPT_CORE_SRC)
	$(MAKE) -C ../clojure install

graalvm:: $(GRAALVM_PATH)

$(GRAALVM_PATH): $(GRAALVM_DOWNLOAD)
	tar xzf $<
	mv graalvm-* $@

$(GRAALVM_DOWNLOAD):
	curl -L -o $@ $(GRAALVM_URL)

# REPL/nREPL management targets
repl:: repl-deps
ifneq (,$(wildcard .nrepl-pid))
	@echo "Connecting to nREPL server on port $$(< .nrepl-port)"
	lein repl :connect
endif

repl-deps::

nrepl: .nrepl-pid
	@echo "nREPL server running on port $$(< .nrepl-port)"

nrepl-stop:
ifneq (,$(wildcard .nrepl-pid))
	-@( \
	  pid=$$(< .nrepl-pid); \
	  read -r pid1 <<<"$$(ps --ppid $$pid -o pid=)"; \
	  read -r pid2 <<<"$$(ps --ppid $$pid1 -o pid=)"; \
	  set -x; \
	  kill -9 $$pid $$pid1 $$pid2; \
	)
	$(RM) .nrepl-*
endif

ifdef PORT
  repl-port := :port $(PORT)
endif

.nrepl-pid:
	( \
	  lein repl :headless $(repl-port) & \
	  echo $$! > $@ \
	)
	@( \
	  while [[ ! -f .nrepl-port ]]; do \
	    sleep 0.3; \
	  done; \
	)
