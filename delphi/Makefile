include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/fpc.mk
include $(MAKES)/shell.mk

ifeq ($(OS-NAME),windows)
# fpc.exe finds the installer generated fpc.cfg next to itself.
# libys.dll is 64 bit, so select the x86_64-win64 cross-compiler:
FPC-OPTS := -Px86_64
else
FPC-OPTS := -n @$(FPC-CFG)
endif


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(FPC)
	fpc $(FPC-OPTS) -Fusrc -Fl$(LIBYS-LIB) \
	    -otest/test_yamlscript test/test_yamlscript.pas
	test/test_yamlscript

release:
	$(ROOT)/util/release-delphi

clean::
	$(RM) src/*.o src/*.ppu test/*.o test/*.ppu test/test_yamlscript
