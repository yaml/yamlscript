RAPIDYAML_VERSION ?= 0.7.2
RAPIDYAML_TAG ?= v$(RAPIDYAML_TAG)
RAPIDYAML_REPO := https://github.com/biojppm/rapidyaml
RAPIDYAML_JAVA := \
  $(ROOT)/rapidyaml/src/main/java/org/rapidyaml/Rapidyaml.java \
  $(ROOT)/rapidyaml/src/main/java/org/rapidyaml/YamlParseErrorException.java
