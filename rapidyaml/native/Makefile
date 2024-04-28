include ../../common/python.mk
include ../rapidyaml.mk

RAPIDYAML_HPP := ryml_all.hpp
RAPIDYAML_AMALGAMATE_OPTS := --stl

build: $(RAPIDYAML_HPP) CMakeLists.txt $(wildcard ./*pp)
	cmake -S . -B rapidyaml-build -D BUILD_SHARED_LIBS=ON
	cmake --build rapidyaml-build --parallel --target rapidyaml

test: build
	cmake --build rapidyaml-build --parallel --target rapidyaml-test
	./rapidyaml-build/rapidyaml-test

$(RAPIDYAML_HPP): rapidyaml
	$(PYTHON) rapidyaml/tools/amalgamate.py $(RAPIDYAML_AMALGAMATE_OPTS) > $@

rapidyaml:
	git clone --recursive $(RAPIDYAML_REPO) -b $(RAPIDYAML_TAG)

clean:
	$(RM) -r rapidyaml-build
	$(RM) -r rapidyaml-install
	$(RM) $(RAPIDYAML_HPP)

realclean: clean
	$(RM) -r rapidyaml
