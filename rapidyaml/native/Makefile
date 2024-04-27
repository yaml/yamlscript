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


#------------------------------------------------------------------------------
default::

build:: $(RAPIDYAML_SO)

test:: build
	cmake --build rapidyaml-build \
	  --parallel --target rapidyaml-test
	./rapidyaml-build/rapidyaml-test

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
	$(RM) -r rapidyaml-build
	$(RM) -r rapidyaml-install

realclean:: clean
	$(RM) -r rapidyaml


#------------------------------------------------------------------------------
$(RAPIDYAML_SO): $(RAPIDYAML_SO_DEPS)
	cmake -S . -B rapidyaml-build \
	  $(CMK_FLAGS)
	cmake --build rapidyaml-build \
	  --parallel --verbose --target rapidyaml
	mv rapidyaml-build/*.so $@
	ln -fs $@ librapidyaml.so

$(RAPIDYAML_H): $(RAPIDYAML_JAVA)
	javac -h . $^

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py \
	  $(RAPIDYAML_AMALGAMATE_OPTS) > $@
