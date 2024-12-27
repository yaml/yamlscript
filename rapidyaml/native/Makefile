include ../../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include $(COMMON)/python.mk

# TODO change to static library!
# https://www.graalvm.org/latest/reference-manual/native-image/guides/build-static-executables/
# https://www.blog.akhil.cc/static-jni
# https://stackoverflow.com/questions/24493337/linking-static-library-with-jni

THIS_DIR := $(shell pwd)
MUSL_DIR := $(THIS_DIR)/_build/x86_64-linux-musl-cross
BDIR := $(THIS_DIR)/_build/shared$(RAPIDYAML_SHARED)-musl$(RAPIDYAML_MUSL)-timed$(RAPIDYAML_TIMED)
BDIR_ST := $(BDIR)-static
BDIR_SH := $(BDIR)-shared

RAPIDYAML_H := org_rapidyaml_Rapidyaml.h
RAPIDYAML_HPP := rapidyaml_all.hpp
RAPIDYAML_AMALGAMATE_OPTS := --stl

RAPIDYAML_DEPS_CFG := \
  Makefile \
  CMakeLists.txt \
  $(JAVA_HOME) \

RAPIDYAML_DEPS_SRC := \
  $(RAPIDYAML_HPP) \
  $(RAPIDYAML_H) \
  $(wildcard ./*pp) \

CMK_ENV :=
CMK_FLAGS :=
CMK_FLAGS += -D YS2EDN_TIMED=$(RAPIDYAML_TIMED)

ifneq ($(RAPIDYAML_MUSL),0)
  CMK_ENV_STATIC := MUSL_DIR=$(MUSL_DIR)
  CMK_FLAGS_STATIC := \
    -D CMAKE_TOOLCHAIN_FILE=musl.x86_64.cmake \
    -D CMAKE_C_FLAGS='-static' \
    -D CMAKE_CXX_FLAGS='-static'
endif


#------------------------------------------------------------------------------

default::

cfg: cfg-static cfg-shared
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
	$(RM) $(RAPIDYAML_HPP)
	$(RM) -r $(BDIR)
	$(RM) -r rapidyaml-install

realclean:: clean
	$(RM) -r rapidyaml


#------------------------------------------------------------------------------

cfg-static: $(BDIR_ST)/CMakeCache.txt
cfg-shared: $(BDIR_SH)/CMakeCache.txt

build-static: $(RAPIDYAML_LIB)
build-shared: $(RAPIDYAML_SO)

test-static: build-static
	cmake --build $(BDIR_ST) --parallel --verbose --target rapidyaml-test-run

test-shared: build-shared
	cmake --build $(BDIR_SH) --parallel --verbose --target rapidyaml-test-run

$(RAPIDYAML_LIB): cfg-static $(RAPIDYAML_DEPS_SRC)
	cmake --build $(BDIR_ST) --parallel --verbose --target rapidyaml
	cp -fv $(BDIR_ST)/*.a $@

$(RAPIDYAML_SO): cfg-shared $(RAPIDYAML_DEPS_SRC)
	cmake --build $(BDIR_SH) --parallel --verbose --target rapidyaml
	cp -fv $(BDIR_SH)/*.so $@

$(BDIR_ST)/CMakeCache.txt: $(RAPIDYAML_DEPS_CFG) $(RAPIDYAML_HPP)
	mkdir -p `dirname $(BDIR_ST)`
	$(CMK_ENV_STATIC) cmake -S . -B $(BDIR_ST) $(CMK_FLAGS) $(CMK_FLAGS_STATIC) -D BUILD_SHARED_LIBS=OFF

$(BDIR_SH)/CMakeCache.txt: $(RAPIDYAML_DEPS_CFG) $(RAPIDYAML_HPP)
	mkdir -p `dirname $(BDIR_SH)`
	cmake -S . -B $(BDIR_SH) $(CMK_FLAGS) -D BUILD_SHARED_LIBS=ON


#------------------------------------------------------------------------------

ifneq ($(RAPIDYAML_MUSL),0)
$(BDIR_ST)/CMakeCache.txt: $(MUSL_DIR)
$(MUSL_DIR):
	mkdir -p `dirname $(MUSL_DIR)`
	cd `dirname $(MUSL_DIR)` && \
	  wget https://musl.cc/x86_64-linux-musl-cross.tgz && \
	  tar xfz x86_64-linux-musl-cross.tgz
endif

$(RAPIDYAML_H): $(RAPIDYAML_JAVA)
	javac -h . $(RAPIDYAML_JAVA) # $^ doesn't work

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py $(RAPIDYAML_AMALGAMATE_OPTS) > $@
