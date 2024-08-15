include ../../common/python.mk
include ../rapidyaml.mk

RAPIDYAML_HPP := rapidyaml_all.hpp
RAPIDYAML_AMALGAMATE_OPTS := --stl

# TODO static library! https://stackoverflow.com/questions/24493337/linking-static-library-with-jni
SHARED := 1
TIMED := 1

CMK_FLAGS :=
CMK_FLAGS += -D YS2EDN_TIMED=$(TIMED)
CMK_FLAGS += -D BUILD_SHARED_LIBS=$(SHARED)


build: $(RAPIDYAML_HPP) $(wildcard ./*pp) CMakeLists.txt Makefile
	cmake -S . -B rapidyaml-build $(CMK_FLAGS)
	cmake --build rapidyaml-build --parallel --target rapidyaml

test: build
	cmake --build rapidyaml-build --parallel --target rapidyaml-test
	./rapidyaml-build/rapidyaml-test

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py $(RAPIDYAML_AMALGAMATE_OPTS) > $@

rapidyaml:
	mkdir rapidyaml
	( cd rapidyaml && git init -q . )
	( cd rapidyaml && git remote add origin $(RAPIDYAML_REPO) )
	( cd rapidyaml && git fetch origin $(RAPIDYAML_TAG) )
	( cd rapidyaml && git reset --hard FETCH_HEAD )
	( cd rapidyaml && git submodule update --init --recursive )

clean:
	$(RM) -r rapidyaml-build
	$(RM) -r rapidyaml-install
	$(RM) $(RAPIDYAML_HPP)

realclean: clean
	$(RM) -r rapidyaml
	$(RM) -r $(RAPIDYAML_HPP)
