include ./rapidyaml.mk
include ../common/base.mk
include $(COMMON)/clojure.mk
include $(COMMON)/java.mk

RAPIDYAML_SOURCES := \
  $(wildcard \
  Makefile \
  native/*pp \
  native/CMakeLists.txt \
  native/Makefile \
)

#------------------------------------------------------------------------------

build:: $(RAPIDYAML_SO)

install test:: build $(JAVA_INSTALLED)
	mvn $@

test-x: build
	mvn -X -e test

$(RAPIDYAML_SO): $(RAPIDYAML_SOURCES)
	$(MAKE) -C native build
	cp -fav native/rapidyaml-build/librapidyaml.so $@

clean::
	$(MAKE) -C native $@
	$(RM) -r reports/ target/ native/rapidyaml/ $(RAPIDYAML_SO)

realclean::
	$(RM) -r $(MAVEN_REPOSITORY)/org/rapidyaml
