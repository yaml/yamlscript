include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/gnat.mk
include $(MAKES)/shell.mk

ALR ?= alr

#------------------------------------------------------------------------------
build:: build-doc obj/yamlscript.o

build-doc:: ReadMe.md

obj/yamlscript.o: src/yamlscript.adb src/yamlscript.ads | obj
	gnatmake -c -Dobj -Isrc src/yamlscript.adb

test:: $(LIBYS-SO-FQNP) $(GNATMAKE) | obj
	gnatmake -Dobj -Isrc test/test_yamlscript.adb \
	    -o test/test_yamlscript \
	    -largs -L$(ROOT)/libys/lib -lys
	test/test_yamlscript

obj:
	mkdir -p $@

clean::
	$(RM) -r obj test/test_yamlscript *.ali *.o
