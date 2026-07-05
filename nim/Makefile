include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/nim.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(NIM)
	nim c -r --hints:off --nimcache:.nimcache -p:src \
	    -o:tests/test_yamlscript.bin tests/test_yamlscript.nim

release:
	$(ROOT)/util/release-nim

clean::
	$(RM) -r .nimcache tests/test_yamlscript.bin
