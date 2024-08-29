include ../../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk
include $(COMMON)/python.mk
include ../rapidyaml.mk

RAPIDYAML_H := org_rapidyaml_Rapidyaml.h
RAPIDYAML_HPP := rapidyaml_all.hpp
RAPIDYAML_AMALGAMATE_OPTS := --stl

RAPIDYAML_SO_DEPS := \
  Makefile \
  CMakeLists.txt \
  $(JAVA_HOME) \
  $(RAPIDYAML_H) \
  $(RAPIDYAML_HPP) \
  $(wildcard ./*pp) \

# TODO maybe change to static library!
# https://stackoverflow.com/questions/24493337/linking-static-library-with-jni
SHARED := 1
TIMED := 0

CMK_FLAGS :=
CMK_FLAGS += -D YS2EDN_TIMED=$(TIMED)
CMK_FLAGS += -D BUILD_SHARED_LIBS=$(SHARED)

BDIR := rapidyaml-build

CMK_BUILD := cmake --build $(BDIR) --parallel --verbose


#------------------------------------------------------------------------------
default::

build:: $(RAPIDYAML_SO)

test:: build
	$(CMK_BUILD) --target rapidyaml-test-run
	./$(BDIR)/rapidyaml-test

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
$(RAPIDYAML_SO): $(RAPIDYAML_SO_DEPS)
	cmake -S . -B $(BDIR) $(CMK_FLAGS)
	$(CMK_BUILD) --target rapidyaml
	mv $(BDIR)/*.so $@
	ln -fs $@ librapidyaml.so

$(RAPIDYAML_H): $(RAPIDYAML_JAVA)
	javac -h . $^

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py $(RAPIDYAML_AMALGAMATE_OPTS) > $@
