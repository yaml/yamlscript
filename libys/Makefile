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

ifeq ($(YAMLSCRIPT_ENGINE),glojure)
test:: test-abi

test-abi: $(LIBYS-SO-FQNP)

# This target tests unpacked release artifacts without rebuilding libys.
test-abi test-installed-abi:
	$(CC) -Wall -Wextra -Werror $(if $(filter windows,$(OS-NAME)),,-pthread) \
	  -I$(LIBYS-LIB) -DLIBYS_HEADER='"$(notdir $(LIBYS-API-HEADER))"' \
	  test/abi.c $(LIBYS-SO-FQNP) -o $(LIBYS-LIB)/test-abi
	PATH="$(LIBYS-LIB):$$PATH" $(LIBYS-LIB)/test-abi

test-handles: $(GLOAT)
	mkdir -p $(ROOT)/.cache/libys-handle-tests
	cp gloat/native/* gloat/include/*.h $(ROOT)/.cache/libys-handle-tests/
	printf 'module libys-handle-tests\n\ngo 1.24.0\n\nrequire github.com/glojurelang/glojure v%s\n' \
	  $(GLOJURE-VERSION) > $(ROOT)/.cache/libys-handle-tests/go.mod
	$(MAKE) --no-print-directory -s -C $(GLOAT-DIR) path-go \
	  > $(ROOT)/.cache/libys-handle-tests/go-path
	cd $(ROOT)/.cache/libys-handle-tests && \
	  go_bin=$$(tail -n1 go-path) && \
	  if [ -n "$$GLOJURE_DIR" ]; then \
	    "$$go_bin" mod edit \
	      -replace github.com/glojurelang/glojure="$$GLOJURE_DIR"; \
	  fi && \
	  GOPATH=$(LOCAL-CACHE)/libys-go \
	  GOCACHE=$(LOCAL-CACHE)/libys-go-build \
	    "$$go_bin" test -mod=mod -race -v .
endif

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

GLOAT-LIBYS-NATIVE := $(wildcard gloat/native/* gloat/include/*.h)

$(LIBYS-SO-GLOJURE): \
  $(GLOAT-RUN-DEPS) $(GLOJURE-PREPARED-STAMP) $(GLOAT-LIBYS-SRCS) \
  $(GLOAT-LIBYS-NATIVE)
ifneq (,$(GLOAT-CROSS))
ifeq (default,$(origin CC))
	$(error Cross compiling libys requires CROSS_CC_TARGET or CC)
endif
endif
	mkdir -p $(dir $@)
	mkdir -p $(ROOT)/.cache/libys-native
	cp gloat/native/* gloat/include/*.h $(ROOT)/.cache/libys-native/
	</dev/null env -u GOROOT \
	  CC="$(CC)" \
	  CGO_ENABLED=1 \
	  GLOAT_YS_RUNTIME=all \
	  GLOAT_MODULE=github.com/yaml/yamlscript \
	  GLOAT_EXTRA_GO_DIR=$(GO-YAML-GENERATED-DIR) \
	  GLOAT_EXTRA_GO_MAIN_DIR=$(ROOT)/.cache/libys-native \
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
ifeq ($(YAMLSCRIPT_ENGINE),graalvm)
	lein with-profile +graalvm uberjar
else
	lein uberjar
endif
	touch $@

$(CORE-INSTALLED):
	$(MAKE) -C $(CORE-DIR) $@
