include ../../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include $(COMMON)/python.mk

RAPIDYAML_H := org_rapidyaml_Rapidyaml.h
RAPIDYAML_HPP := rapidyaml_all.hpp
RAPIDYAML_AMALGAMATE_OPTS := --stl

RAPIDYAML_DEPS := \
  Makefile \
  CMakeLists.txt \
  $(JAVA_HOME) \
  $(RAPIDYAML_H) \
  $(RAPIDYAML_HPP) \
  $(wildcard ./*pp) \

CMK_FLAGS :=
CMK_FLAGS += -D YS2EDN_TIMED=$(RAPIDYAML_TIMED)

BDIR := rapidyaml-build

CMK_BUILD :=


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
	$(RM) $(RAPIDYAML_HPP)
	$(RM) -r $(BDIR)
	$(RM) -r rapidyaml-install

realclean:: clean
	$(RM) -r rapidyaml


#------------------------------------------------------------------------------
build-static: $(RAPIDYAML_LIB)
test-static: build-static
	cmake --build $(BDIR)-static --parallel --verbose --target rapidyaml-test-run
$(RAPIDYAML_LIB): $(RAPIDYAML_DEPS)
	cmake -S . -B $(BDIR)-static $(CMK_FLAGS) -D BUILD_SHARED_LIBS=OFF
	cmake --build $(BDIR)-static --parallel --verbose --target rapidyaml
	cp -fv $(BDIR)-static/*.a $@

build-shared: $(RAPIDYAML_SO)
test-shared: build-shared
	cmake --build $(BDIR)-shared --parallel --verbose --target rapidyaml-test-run
$(RAPIDYAML_SO): $(RAPIDYAML_DEPS)
	cmake -S . -B $(BDIR)-shared $(CMK_FLAGS) -D BUILD_SHARED_LIBS=ON
	cmake --build $(BDIR)-shared --parallel --verbose --target rapidyaml
	cp -fv $(BDIR)-shared/*.so $@
	ln -fs $@ librapidyaml.so

$(RAPIDYAML_H): $(RAPIDYAML_JAVA)
	javac -h . $(RAPIDYAML_JAVA) # $^ doesn't work

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py $(RAPIDYAML_AMALGAMATE_OPTS) > $@
