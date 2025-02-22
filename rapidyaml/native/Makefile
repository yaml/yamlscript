include ../../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include $(COMMON)/python.mk

# TODO change to static library!
# https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/
# https://www.blog.akhil.cc/static-jni
# https://stackoverflow.com/questions/24493337/linking-static-library-with-jni

THIS_DIR := $(shell pwd)
BDIR := $(THIS_DIR)/_build/$(RAPIDYAML_BUILD_TYPE)-shared$(RAPIDYAML_SHARED)-timed$(RAPIDYAML_TIMED)-dbg$(RAPIDYAML_DBG)

RAPIDYAML_DEPS := \
  Makefile \
  CMakeLists.txt \
  $(JAVA_HOME) \
  $(RAPIDYAML_JNI_H) \
  $(wildcard ./*pp) \

CMK_ENV ?=
CMK_FLAGS_EXTRA ?=
CMK_FLAGS := \
  -D CMAKE_BUILD_TYPE=$(RAPIDYAML_BUILD_TYPE) \
  -D YSPARSE_TIMED=$(RAPIDYAML_TIMED) \
  -D YSPARSE_DBG=$(RAPIDYAML_DBG) \
  -D CMAKE_EXPORT_COMPILE_COMMANDS=ON


#------------------------------------------------------------------------------
default::

build:: build-static build-shared
test: test-static test-shared

rapidyaml:
	mkdir -p $@
	git -C $@ init -q .
	git -C $@ remote add origin $(RAPIDYAML_REPO)
	git -C $@ fetch origin $(RAPIDYAML_TAG)
	git -C $@ reset --hard FETCH_HEAD
	git -C $@ submodule update --init --recursive

clean::
	$(RM) librapidyaml.*
	$(RM) -r $(BDIR)
	$(RM) -r rapidyaml-install

realclean:: clean
	$(RM) -r rapidyaml


#------------------------------------------------------------------------------

$(RAPIDYAML_LIB): rapidyaml
build-static: $(RAPIDYAML_LIB)
test-static: build-static
	$(CMK_ENV) cmake --build $(BDIR)-static --parallel --verbose --target rapidyaml-test-run
$(RAPIDYAML_LIB): $(RAPIDYAML_DEPS)
	$(CMK_ENV) cmake -S . -B $(BDIR)-static $(CMK_FLAGS) $(CMK_FLAGS_EXTRA) -D BUILD_SHARED_LIBS=OFF
	cmake --build $(BDIR)-static --parallel --verbose --target rapidyaml
	cp -fv $(BDIR)-static/*.a $@

$(RAPIDYAML_SO): rapidyaml
build-shared: $(RAPIDYAML_SO)
test-shared: build-shared
	cmake --build $(BDIR)-shared --parallel --verbose --target rapidyaml-test-run
$(RAPIDYAML_SO): $(RAPIDYAML_DEPS)
	$(CMK_ENV) cmake -S . -B $(BDIR)-shared $(CMK_FLAGS) $(CMK_FLAGS_EXTRA) -D BUILD_SHARED_LIBS=ON
	$(CMK_ENV) cmake --build $(BDIR)-shared --parallel --verbose --target rapidyaml
	cp -fv $(BDIR)-shared/*.so $@
	ln -fs $@ librapidyaml.so

$(RAPIDYAML_JNI_H): $(RAPIDYAML_JAVA)
	javac -h . $(RAPIDYAML_JAVA) # $^ doesn't work
