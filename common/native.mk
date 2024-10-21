NATIVE_OPTS := \
  -O$(GRAALVM_O) \
  --verbose \
  --native-image-info \
  --no-fallback \
  --initialize-at-build-time \
  --enable-preview \
  --enable-url-protocols=https \
  -H:ReflectionConfigurationFiles=reflection.json \
  -H:+ReportExceptionStackTraces \
  -H:IncludeResources=SCI_VERSION \
  -H:Log=registerResource: \
  -J-Dclojure.spec.skip-macros=true \
  -J-Dclojure.compiler.direct-linking=true \
  -J-Xmx3g \

export MUSL_HOME := $(YS_TMP)/musl
export PATH := $(MUSL_HOME)/bin:$(PATH)

MUSL_GCC := $(MUSL_HOME)/bin/musl-gcc

MUSL_VERSION := 1.2.4
MUSL_TAR := musl-$(MUSL_VERSION).tar.gz
MUSL_DIR := $(YS_TMP)/musl-$(MUSL_VERSION)
MUSL_URL := https://musl.libc.org/releases/$(MUSL_TAR)

ZLIB_VERSION := 1.2.13
ZLIB_TAR := zlib-$(ZLIB_VERSION).tar.gz
ZLIB_DIR := $(YS_TMP)/zlib-$(ZLIB_VERSION)
ZLIB_URL := https://zlib.net/fossils/$(ZLIB_TAR)

#-------------------------------------------------------------------------------
$(MUSL_GCC): | $(MUSL_HOME)
	ln -s $@ $(MUSL_HOME)/bin/x86_64-linux-musl-gcc
	musl-gcc --version

$(MUSL_HOME): | $(MUSL_DIR) $(ZLIB_DIR)
	(cd $(MUSL_DIR) && \
	  ./configure --prefix=$@ --static && \
	  make && make install)
	(cd $(ZLIB_DIR) && \
	  CC=musl-gcc ./configure --prefix=$@ --static && \
	  make && make install)

$(MUSL_DIR): | $(YS_TMP)/$(MUSL_TAR)
	(cd $(YS_TMP) && tar -xf $(YS_TMP)/$(MUSL_TAR))

$(YS_TMP)/$(MUSL_TAR):
	curl -o $(YS_TMP)/$(MUSL_TAR) $(MUSL_URL)

$(ZLIB_DIR): | $(YS_TMP)/$(ZLIB_TAR)
	(cd $(YS_TMP) && tar -xf $(YS_TMP)/$(ZLIB_TAR))

$(YS_TMP)/$(ZLIB_TAR):
	curl -o $(YS_TMP)/$(ZLIB_TAR) $(ZLIB_URL)

muslclean::
	$(RM) -r $(MUSL_HOME) $(MUSL_DIR) $(MUSL_TAR) $(ZLIB_DIR) $(ZLIB_TAR)
