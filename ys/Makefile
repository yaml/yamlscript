include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-cli.mk
include $(COMMON)/docker.mk

include $(MAKES)/shell.mk

BPAN-LOCAL := .bpan
BPAN-REPO-URL := https://github.com/bpan-org/bpan

test := test/

# override PATH := $(ROOT)/ys/bin:$(PATH)
# export PATH


#------------------------------------------------------------------------------
build:: $(CLI-DEPS)

install: $(CLI-DEPS)
	mkdir -p $(PREFIX)/bin
	install -m 755 $(CLI-BIN) $(PREFIX)/bin/
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $(CLI-BIN)) \
	  $(PREFIX)/$(CLI-BIN:%-$(YS_VERSION)=%)

jar: $(CLI-JAR)

test: test-run

test-all: test-unit test-run

test-unit: $(LEIN) $(CORE-INSTALLED)
	lein test $(lein-test)

test-run: $(CLI-DEPS) $(BPAN-LOCAL)
	prove $${TEST_VERBOSE:+'-v'} $(test)

repl-deps:: $(CORE-INSTALLED)

clean::
	$(RM) -r .cpcache/
	$(RM) $(YS-BUILD-LOG)

realclean::
	$(RM) -r bin lib $(BPAN-LOCAL)

$(BPAN-LOCAL):
	git clone --depth=1 $(BPAN-REPO-URL) $@

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
	  native-image \
	    -H:ConfigurationFileDirectories=config/ \
	    $(NATIVE-OPTIONS) \
	    -jar $< \
	    -o $@ \
	) 2>&1 | tee $(YS-BUILD-LOG)
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%-$(API_VERSION))
	ln -fs $(notdir $@) $(@:%-$(YS_VERSION)=%)
	touch $@
endif

$(CLI-JAR): $(LEIN) $(CLI-JAR-DEPS)
	lein uberjar

$(CORE-INSTALLED): $(CORE-JAR)
	@$(MAKE) -C $(CORE-DIR) $@ > /dev/null 2>&1

$(CORE-JAR):
	@$(MAKE) -C $(CORE-DIR) >/dev/null 2>&1
