include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/d.mk
include $(MAKES)/shell.mk

export DUB_HOME := $(ROOT)/.cache/dub

DUB := $(LDC-BIN)/dub


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(LDC)
	dub test

release:
	$(ROOT)/util/release-d

clean::
	$(RM) -r .dub dub.selections.json yamlscript-test-library
