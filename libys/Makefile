include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-core.mk
include $(COMMON)/vars-libys.mk
include $(COMMON)/docker.mk
include $(COMMON)/glojure.mk

include $(MAKES)/shell.mk

#------------------------------------------------------------------------------
build:: $(LIBYS-DEPS)

build-glojure:
	$(MAKE) build YAMLSCRIPT_ENGINE=glojure

build-graalvm:
	$(MAKE) build YAMLSCRIPT_ENGINE=graalvm

jar: $(LIBYS-JAR-PATH)

install:: $(LIBYS-DEPS)
	mkdir -p $(PREFIX)/include/
	install -m 644 $(LIBYS-HEADERS) $(PREFIX)/include/
	mkdir -p $(PREFIX)/lib/
	install -m 644 $< $(PREFIX)/lib/
ifneq ($(OS-NAME),windows)
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS-SO-BASE))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS-SO-APIP))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS-SO-VERS))
endif

clean::
	$(RM) $(LIBYS-BUILD-LOG)

realclean::
	$(RM) -r lib/
	$(RM) src/libys/Core.class

test:: $(LIBYS-SO-FQNP)

repl-deps:: $(LIBYS-JAR-PATH)

ifeq ($(YAMLSCRIPT_ENGINE),graalvm)
ifndef PROPER-BUILD-ENV
$(LIBYS-SO-GRAALVM): $(DOCKER-RUN-FILE)
	@$(DOCKER-EXEC) make -C libys $@
else

$(LIBYS-SO-GRAALVM): $(LIBYS-JAR-PATH) $(REFLECTION-JSON)
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
	    $(NATIVE-OPTS) \
	    --shared \
	    -jar $< \
	    -o $(basename $@) \
	) 2>&1 | tee $(LIBYS-BUILD-LOG)
	test -f $@
endif

else ifeq ($(YAMLSCRIPT_ENGINE),glojure)

GLOAT-LIBYS-SRCS := \
  $(GLOJURE-SRCS) \
  gloat/src/libys.clj

$(LIBYS-SO-GLOJURE): \
  $(GLOAT) $(GLOJURE-PREPARED-STAMP) $(GLOAT-LIBYS-SRCS)
	mkdir -p $(dir $@)
	</dev/null env -u GOROOT \
	  GLOAT_YS_RUNTIME=all \
	  GLOAT_MODULE=github.com/yaml/yamlscript \
	  GLOAT_EXTRA_GO_DIR=$(GO-YAML-GENERATED-DIR) \
	  GLOAT_ENGINE=$(GLOAT-ENGINE) \
	  $(GLOAT-RUN) --force $(GLOAT-PLATFORM-OPT) \
	  $(GLOAT-LIBYS-SRCS) -o $@
	test -f $@

else
$(error Unknown YAMLSCRIPT_ENGINE '$(YAMLSCRIPT_ENGINE)')
endif

$(LIBYS-SO-FQNP): $(LIBYS-SO-ENGINE)
	mkdir -p $(dir $@)
ifeq ($(YAMLSCRIPT_ENGINE),glojure)
	cp $(LIBYS-GLOJURE-HEADER) $(LIBYS-API-HEADER)
	cp $(LIBYS-GLOJURE-ISOLATE-HEADER) $(LIBYS-LIB)/graal_isolate.h
else
	cp $(basename $(LIBYS-SO-GRAALVM)).h $(LIBYS-API-HEADER)
endif
ifneq (,$(or $(filter windows,$(OS-NAME)),$(GLOAT-WINDOWS-TARGET)))
	cp $< $@
else
	ln -fs $(notdir $<) $@
	ln -fs $(notdir $<) $(LIBYS-SO-APIP)
	ln -fs $(notdir $<) $(LIBYS-SO-VERS)
	ln -fs $(notdir $<) $(LIBYS-SO-BASE)
endif

$(LIBYS-JAR-PATH): $(CORE-INSTALLED)
	lein uberjar
	touch $@

$(CORE-INSTALLED):
	$(MAKE) -C $(CORE-DIR) $@
