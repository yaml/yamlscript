include ../../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include $(COMMON)/python.mk

# TODO change to static library!
# https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/
# https://www.blog.akhil.cc/static-jni
# https://stackoverflow.com/questions/24493337/linking-static-library-with-jni

THIS_DIR := $(shell pwd)
BUILD_ROOT := $(THIS_DIR)/_build
MUSL_DIR := $(BUILD_ROOT)/x86_64-linux-musl-cross
BDIR := $(BUILD_ROOT)/$(RAPIDYAML_BUILD_TYPE)-musl$(RAPIDYAML_MUSL)-timed$(RAPIDYAML_TIMED)-dbg$(RAPIDYAML_DBG)

RAPIDYAML_DEPS := \
  Makefile \
  CMakeLists.txt \
  $(JAVA_HOME) \
  $(RAPIDYAML_JNI_H) \
  $(wildcard ./*pp) \

CMK_ENV ?=
CMK_ENV_STATIC ?=
CMK_ENV_SHARED ?=
CMK_FLAGS := \
  -D CMAKE_BUILD_TYPE=$(RAPIDYAML_BUILD_TYPE) \
  -D YSPARSE_TIMED=$(RAPIDYAML_TIMED) \
  -D YSPARSE_DBG=$(RAPIDYAML_DBG) \
  -D CMAKE_EXPORT_COMPILE_COMMANDS=ON
CMK_FLAGS_STATIC ?=
CMK_FLAGS_SHARED ?=
CMK_FLAGS_EXTRA ?=
ifneq ($(RAPIDYAML_MUSL),0)
  CMK_ENV += MUSL_DIR=$(MUSL_DIR)
  CMK_FLAGS_STATIC += \
    -D CMAKE_TOOLCHAIN_FILE=musl.x86_64.cmake \
    -D CMAKE_C_FLAGS='-static' \
    -D CMAKE_CXX_FLAGS='-static'
endif


#------------------------------------------------------------------------------
default::

cfg:: cfg-static cfg-shared
build:: build-static build-shared
test: test-static test-shared

rapidyaml:
	mkdir -p $@
	$(GIT) -C $@ init -q .
	$(GIT) -C $@ remote add origin $(RAPIDYAML_REPO)
	$(GIT) -C $@ fetch origin $(RAPIDYAML_TAG)
	$(GIT) -C $@ reset --hard FETCH_HEAD
	$(GIT) -C $@ submodule update --init --recursive

clean::
	$(RM) librapidyaml.*
	$(RM) -r _build
	$(RM) -r rapidyaml-install

realclean:: clean
	$(RM) -r rapidyaml

jni: $(RAPIDYAML_JNI_H)

jnicheck: jni
	$(GIT) diff --exit-code $(RAPIDYAML_JNI_H)


#------------------------------------------------------------------------------

build-static: $(RAPIDYAML_LIB)
cfg-static: rapidyaml
	$(CMK_ENV) $(CMK_ENV_STATIC) $(CMAKE) -S . -B $(BDIR)-static $(CMK_FLAGS) $(CMK_FLAGS_STATIC) $(CMK_FLAGS_EXTRA) -D BUILD_SHARED_LIBS=OFF
test-static: build-static
	$(CMK_ENV) $(CMK_ENV_STATIC) $(CMAKE) --build $(BDIR)-static --verbose --target rapidyaml-test-run
test-static-timing: build-static
	$(CMK_ENV) $(CMK_ENV_STATIC) $(CMAKE) --build $(BDIR)-static --verbose --target rapidyaml-test-run-timing
$(RAPIDYAML_LIB): cfg-static $(RAPIDYAML_DEPS)
	$(CMK_ENV) $(CMK_ENV_STATIC) $(CMAKE) --build $(BDIR)-static --verbose --parallel --target rapidyaml
	cp -fv $(BDIR)-static/*.a $@

build-shared: $(RAPIDYAML_SO)
cfg-shared: rapidyaml
	$(CMK_ENV) $(CMK_ENV_SHARED) $(CMAKE) -S . -B $(BDIR)-shared $(CMK_FLAGS) $(CMK_FLAGS_SHARED) $(CMK_FLAGS_EXTRA) -D BUILD_SHARED_LIBS=ON
test-shared: build-shared
	$(CMK_ENV) $(CMK_ENV_SHARED) $(CMAKE) --build $(BDIR)-shared --verbose --target rapidyaml-test-run
test-shared-timing: build-shared
	$(CMK_ENV) $(CMK_ENV_SHARED) $(CMAKE) --build $(BDIR)-shared --verbose --target rapidyaml-test-run-timing
$(RAPIDYAML_SO): cfg-shared $(RAPIDYAML_DEPS)
	$(CMK_ENV) $(CMK_ENV_SHARED) $(CMAKE) --build $(BDIR)-shared --verbose --parallel --target rapidyaml
	cp -fv $(BDIR)-shared/*.so $@
	ln -fs $@ librapidyaml.so

musl:
ifneq ($(RAPIDYAML_MUSL),0)
musl: $(MUSL_DIR)
cfg-static: $(MUSL_DIR)
cfg-shared: $(MUSL_DIR)
$(MUSL_DIR):
	mkdir -p `dirname $(MUSL_DIR)`
	cd `dirname $(MUSL_DIR)` && \
	  wget https://musl.cc/x86_64-linux-musl-cross.tgz && \
	  tar xfz x86_64-linux-musl-cross.tgz
	ln -fs /usr/lib/ld-musl-x86_64.so.1 $(MUSL_DIR)/bin/ldd
	@# musls dynamic linker is an ldd stand-in
	@# see https://wiki.musl-libc.org/faq
endif

$(RAPIDYAML_JNI_H): $(JAVAC) $(RAPIDYAML_JAVA)
	$(JAVAC) -h . $(RAPIDYAML_JAVA) # $^ doesn't work
