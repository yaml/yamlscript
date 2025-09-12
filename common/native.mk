REFLECTION-JSON := $(COMMON)/reflection.json

NATIVE-OPTS := \
  -O$(GRAALVM-O) \
  --verbose \
  --native-image-info \
  --no-fallback \
  --initialize-at-build-time \
  --enable-preview \
  --enable-url-protocols=https \
  --emit=build-report \
  -march=compatibility \
  -H:+UnlockExperimentalVMOptions \
  -H:+StaticExecutableWithDynamicLibC \
  -H:IncludeResources=SCI_VERSION \
  -H:ReflectionConfigurationFiles=$(REFLECTION-JSON) \
  -H:+ReportExceptionStackTraces \
  -H:Log=registerResource: \
  -J-Dclojure.spec.skip-macros=true \
  -J-Dclojure.compiler.direct-linking=true \
  -J-Xmx3g \

MUSL-HOME := $(LOCAL-CACHE)/musl
export MUSL_HOME := $(MUSL-HOME)
export PATH := $(MUSL-HOME)/bin:$(PATH)

MUSL-GCC := $(MUSL-HOME)/bin/musl-gcc

MUSL-TAR := musl-$(MUSL-VERSION).tar.gz
MUSL-DIR := $(LOCAL-CACHE)/musl-$(MUSL-VERSION)
MUSL-URL := https://musl.libc.org/releases/$(MUSL-TAR)

ZLIB-TAR := zlib-$(ZLIB-VERSION).tar.gz
ZLIB-DIR := $(LOCAL-CACHE)/zlib-$(ZLIB-VERSION)
ZLIB-URL := https://zlib.net/fossils/$(ZLIB-TAR)


#-------------------------------------------------------------------------------
# We need to do this by hand for now because it depends on a working `ys`
# release which isn't always available:
# https://github.com/yaml/yamlscript/issues/258
build-reflection-json: $(YS)
	ys -J $(COMMON)/reflection.ys > $(REFLECTION-JSON)

$(MUSL-GCC): | $(MUSL-HOME)
	ln -s $@ $(MUSL-HOME)/bin/x86_64-linux-musl-gcc
	musl-gcc --version

$(MUSL-HOME): | $(MUSL-DIR) $(ZLIB-DIR)
	(cd $(MUSL-DIR) && \
	  ./configure --prefix=$@ --static && \
	  make && make install)
	(cd $(ZLIB-DIR) && \
	  CC=musl-gcc ./configure --prefix=$@ --static && \
	  make && make install)

$(MUSL-DIR): | $(LOCAL-CACHE)/$(MUSL-TAR)
	(cd $(LOCAL-CACHE) && tar -xf $(LOCAL-CACHE)/$(MUSL-TAR))

$(LOCAL-CACHE)/$(MUSL-TAR):
	$(call need-curl)
	$(CURL) -o $(LOCAL-CACHE)/$(MUSL-TAR) $(MUSL-URL)

$(ZLIB-DIR): | $(LOCAL-CACHE)/$(ZLIB-TAR)
	(cd $(LOCAL-CACHE) && tar -xf $(LOCAL-CACHE)/$(ZLIB-TAR))

$(LOCAL-CACHE)/$(ZLIB-TAR):
	$(call need-curl)
	$(CURL) -o $(LOCAL-CACHE)/$(ZLIB-TAR) $(ZLIB-URL)

muslclean::
	$(RM) -r $(MUSL-HOME) $(MUSL-DIR) $(MUSL-TAR) $(ZLIB-DIR) $(ZLIB-TAR)
