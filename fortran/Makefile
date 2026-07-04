include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/fortran.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(FPM) $(GFORTRAN)
	fpm test --flag "-L$(LIBYS-LIB)"

release:
	$(ROOT)/util/release-fortran

clean::
	$(RM) -r build
