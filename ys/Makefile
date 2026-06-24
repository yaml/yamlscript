include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-cli.mk
include $(COMMON)/docker.mk

include $(MAKES)/perl.mk
include $(MAKES)/shell.mk

BPAN-LOCAL := .bpan
BPAN-REPO-URL := https://github.com/bpan-org/bpan

ifeq ($(OS-NAME),windows)
PROVE := $(PERL) -S prove
else
PROVE := prove
endif

test := test/

YSJ-TMP := .cache/tmp/ysj
YSJ-BIN-DIR := $(YSJ-TMP)/bin
YSJ-BIN := $(YSJ-BIN-DIR)/ys
YSJ-API-BIN := $(YSJ-BIN-DIR)/ys-$(API_VERSION)
YSJ-DOCKER-CONFIG := $(YSJ-TMP)/docker

# override PATH := $(ROOT)/ys/bin:$(PATH)
# export PATH


#------------------------------------------------------------------------------
build:: $(CLI-DEPS)

install: $(CLI-DEPS)
	mkdir -p $(PREFIX)/bin
	install -m 755 $(CLI-BIN) $(PREFIX)/bin/
ifneq ($(OS-NAME),windows)
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%)
	install -m 755 $(CLI-BIN-BASH) \
	  $(PREFIX)/bin/
endif

jar: $(CLI-JAR)

test: test-run

test-all: test-unit test-run

test-unit: $(LEIN) $(CORE-INSTALLED)
	lein test $(lein-test)

ifdef YS_RELEASE_USE_INSTALLED_YS
TEST-RUN-DEPS := $(PERL) $(BPAN-LOCAL)
else
TEST-RUN-DEPS := $(PERL) $(CLI-DEPS) $(BPAN-LOCAL)
endif

test-run: $(TEST-RUN-DEPS)
ifdef YS_RELEASE_USE_INSTALLED_YS
	test -f $(CLI-BIN)
endif
	$(PROVE) $${TEST_VERBOSE:+'-v'} $(test)

test-ysj: export DOCKER_CONFIG := $(abspath $(YSJ-DOCKER-CONFIG))
test-ysj: export PATH := $(abspath $(YSJ-BIN-DIR)):$(PATH)
test-ysj: export YS_BIN_DIR := $(abspath $(YSJ-BIN-DIR))
test-ysj: $(PERL) $(YSJ-BIN) $(YSJ-API-BIN) $(YSJ-DOCKER-CONFIG) \
	  $(BPAN-LOCAL)
	$(PROVE) $${TEST_VERBOSE:+'-v'} $(test)

repl-deps:: $(CORE-INSTALLED)

clean::
	$(RM) -r .cpcache/
	$(RM) $(YS-BUILD-LOG)

realclean::
	$(RM) -r bin lib $(BPAN-LOCAL)

$(BPAN-LOCAL):
	git clone --depth=1 $(BPAN-REPO-URL) $@

$(YSJ-BIN): $(ROOT)/util/ysj
	mkdir -p $(dir $@)
	printf '%s\n' \
	  '#!/usr/bin/env bash' \
	  'exec $(abspath $<) "$$@"' > $@
	chmod 755 $@

$(YSJ-API-BIN): $(YSJ-BIN)
	ln -fs ys $@

$(YSJ-DOCKER-CONFIG):
	mkdir -p $@

ifndef PROPER-BUILD-ENV
$(CLI-BIN): $(DOCKER-RUN-FILE)
	$(DOCKER-EXEC) make -C ys $@
else

$(CLI-BIN): $(CLI-JAR) $(REFLECTION-JSON)
ifneq (true,$(LIBZ))
	$(error *** \
	  The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
	( set -x && \
	  $(TIME) \
	  $(NATIVE-ENV) \
	  $(NATIVE-IMAGE) \
	    -H:ConfigurationFileDirectories=config/ \
	    $(NATIVE-OPTS) \
	    -jar $< \
	    -o $(CLI-OUT) \
	) 2>&1 | tee $(YS-BUILD-LOG)
	test -f $@
ifneq ($(OS-NAME),windows)
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%)
endif
	touch $@
endif

$(CLI-BIN-BASH): $(CLI-BIN-BASH-SRC)
	mkdir -p $(dir $@)
	cp $< $@
	chmod 755 $@

$(CLI-JAR): $(LEIN) $(CLI-JAR-DEPS)
	lein uberjar

$(CORE-INSTALLED): $(CORE-JAR)
	$(MAKE) -C $(CORE-DIR) $@

$(CORE-JAR):
	$(MAKE) -C $(CORE-DIR)
