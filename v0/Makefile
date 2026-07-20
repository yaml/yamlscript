include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk

include $(MAKES)/babashka.mk
include $(MAKES)/perl.mk
include $(MAKES)/shell.mk

export CLOJARS_USERNAME ?= $(shell ../clojure/util/get-setting username)
export CLOJARS_PASSWORD ?= $(shell ../clojure/util/get-setting password)

# The ys executable used to compile test fixtures. Defaults to the JVM
# uberjar runner so tests exercise the current source tree.
export YS_BIN ?= $(ROOT)/util/ysj

ifeq ($(OS-NAME),windows)
PROVE := $(PERL) -S prove
else
PROVE := prove
endif

V0-JAR := ys.v0-$(YS_VERSION).jar
V0-M2 := $(HOME)/.m2/repository/org/yamlscript/ys.v0/$(YS_VERSION)

# data.json is the one ys.v0 backend that babashka does not bundle, so
# the -T bb header's java free add-classpath path needs its jar in
# ~/.m2 too. The lein install above resolves it into the Makes m2:
DATA-JSON-VERSION := \
    $(shell sed -n 's/.*data.json "\(.*\)".*/\1/p' project.clj)
DATA-JSON-JAR := data.json-$(DATA-JSON-VERSION).jar
DATA-JSON-POM := data.json-$(DATA-JSON-VERSION).pom
DATA-JSON-MAKES-M2 := $(ROOT)/.cache/.local/home/.m2/repository
DATA-JSON-MAKES-M2 := \
    $(DATA-JSON-MAKES-M2)/org/clojure/data.json/$(DATA-JSON-VERSION)
DATA-JSON-M2 := \
    $(HOME)/.m2/repository/org/clojure/data.json/$(DATA-JSON-VERSION)

#------------------------------------------------------------------------------

build-doc::

test install deploy:: $(LEIN)
	lein $@

# Also install the ys.v0 and data.json artifacts (and only those; no
# lein resolution ever touches $HOME) into the real ~/.m2, where the
# consuming runtimes (bb, clj, jolt, glojure) look for them. The
# <jar>.d extraction is for source loading runtimes like glojure.
install::
	@( \
	  mkdir -p $(V0-M2) $(DATA-JSON-M2) && \
	  cp target/$(V0-JAR) $(V0-M2)/ && \
	  cp pom.xml $(V0-M2)/ys.v0-$(YS_VERSION).pom && \
	  cp $(DATA-JSON-MAKES-M2)/$(DATA-JSON-JAR) \
	     $(DATA-JSON-MAKES-M2)/$(DATA-JSON-POM) \
	     $(DATA-JSON-M2)/ && \
	  rm -rf $(V0-M2)/$(V0-JAR).d && \
	  ( ! command -v unzip >/dev/null || \
	    unzip -o -q $(V0-M2)/$(V0-JAR) -d $(V0-M2)/$(V0-JAR).d ) && \
	  echo "Installed ys.v0 $(YS_VERSION) into ~/.m2/repository" \
	) 2>/dev/null || \
	  echo "WARNING: could not install ys.v0 into ~/.m2"

test:: test-bb

test-bb: $(BB) $(PERL) install
	$(PROVE) $${TEST_VERBOSE:+'-v'} test/

release: deploy

clean::
	$(RM) pom.xml
	$(RM) -r target/ .cpcache/
