include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/scala.mk
include $(MAKES)/shell.mk

export COURSIER_CACHE := $(ROOT)/.cache/coursier
export JAVA_TOOL_OPTIONS := -Djna.tmpdir=$(ROOT)/.cache/jna


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(SCALA-CLI)
	mkdir -p $(ROOT)/.cache/jna
	scala-cli run --server=false src test

clean::
	$(RM) -r .bsp .scala-build
