include ../../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include $(COMMON)/python.mk
include ../rapidyaml.mk

# TODO change to static library!
# https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/
# https://www.blog.akhil.cc/static-jni
# https://stackoverflow.com/questions/24493337/linking-static-library-with-jni
SHARED := 1
# when SHARED=0, also link statically with the musl libc
MUSL := 1
# enable timing scopes
TIMED := 0

BUILD_TYPE := Release

RAPIDYAML_H := org_rapidyaml_Rapidyaml.h
RAPIDYAML_HPP := rapidyaml_all.hpp
RAPIDYAML_AMALGAMATE_OPTS := --stl

ifeq ($(SHARED),0)
RAPIDYAML_LIB := librapidyaml.a
else
RAPIDYAML_LIB := librapidyaml.$(RAPIDYAML_VERSION).so
endif

RAPIDYAML_LIB_DEPS := \
  Makefile \
  CMakeLists.txt \
  $(JAVA_HOME) \
  $(RAPIDYAML_H) \
  $(RAPIDYAML_HPP) \
  $(wildcard ./*pp) \

THIS_DIR := $(shell pwd)
MUSL_DIR := $(THIS_DIR)/_build/x86_64-linux-musl-cross

CMK_ENV :=
CMK_FLAGS :=
CMK_FLAGS += -D CMAKE_BUILD_TYPE=$(BUILD_TYPE)
CMK_FLAGS += -D YS2EDN_TIMED=$(TIMED)
CMK_FLAGS += -D BUILD_SHARED_LIBS=$(SHARED)
ifeq ($(SHARED),0)
  ifneq ($(MUSL),0)
    CMK_ENV += MUSL_DIR=$(MUSL_DIR)
    CMK_FLAGS += \
      -D CMAKE_TOOLCHAIN_FILE=musl.x86_64.cmake \
      -D CMAKE_C_FLAGS='-static' \
      -D CMAKE_CXX_FLAGS='-static'
  endif
endif

BDIR := $(THIS_DIR)/_build/shared$(SHARED)-musl$(MUSL)-timed$(TIMED)

CMK_BUILD := cmake --build $(BDIR) --parallel --verbose


#------------------------------------------------------------------------------
default::

.PHONY: build
build:: $(RAPIDYAML_LIB)

test_build:: build
	$(CMK_BUILD) --target rapidyaml-test
test:: build
	$(CMK_BUILD) --target rapidyaml-test-run

rapidyaml:
	mkdir -p $@
	git -C $@ init -q .
	git -C $@ remote add origin $(RAPIDYAML_REPO)
	git -C $@ fetch origin $(RAPIDYAML_TAG)
	git -C $@ reset --hard FETCH_HEAD
	git -C $@ submodule update --init --recursive

clean::
	$(RM) librapidyaml.*
	$(RM) $(RAPIDYAML_HPP)
	$(RM) -r $(BDIR)
	$(RM) -r rapidyaml-install

realclean:: clean
	$(RM) -r rapidyaml


#------------------------------------------------------------------------------
$(BDIR)/CMakeCache.txt:
	mkdir -p $(BDIR)
	$(CMK_ENV) cmake -S . -B $(BDIR) $(CMK_FLAGS)

$(RAPIDYAML_LIB): $(RAPIDYAML_LIB_DEPS) $(BDIR)/CMakeCache.txt
	$(CMK_BUILD) --target rapidyaml
	if [ -d $@ ] ; then rm $@ ; fi
ifneq ($(SHARED),0)
	cp -fv $(BDIR)/*.so $@
	ln -fs $@ librapidyaml.so
else
	cp -fv $(BDIR)/*.a $@
endif

ifeq ($(SHARED),0)
ifneq ($(MUSL),0)
$(RAPIDYAML_LIB): $(MUSL_DIR)
$(MUSL_DIR):
	mkdir -p `dirname $(MUSL_DIR)`
	cd `dirname $(MUSL_DIR)` && \
	  wget https://musl.cc/x86_64-linux-musl-cross.tgz && \
	  tar xfz x86_64-linux-musl-cross.tgz
endif
endif

$(RAPIDYAML_H): $(RAPIDYAML_JAVA)
	javac -h . $^

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py $(RAPIDYAML_AMALGAMATE_OPTS) > $@
