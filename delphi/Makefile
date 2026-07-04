include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/fpc.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(FPC)
	fpc -n @$(FPC-CFG) -Fusrc -Fl$(LIBYS-LIB) \
	    -otest/test_yamlscript test/test_yamlscript.pas
	test/test_yamlscript

release:
	$(ROOT)/util/release-delphi

clean::
	$(RM) src/*.o src/*.ppu test/*.o test/*.ppu test/test_yamlscript
