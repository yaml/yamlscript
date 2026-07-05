include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/fortran.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

ifeq ($(OS-NAME),windows)
# fpm.toml links -lys.0.2.26, which the mingw linker resolves to a
# file named libys.0.2.26.dll; provide one as a copy of libys.dll
# and put its directory on PATH for run time:
LIBYS-ALIAS := .libys/libys.$(YAMLSCRIPT_VERSION).dll

test:: $(LIBYS-SO-FQNP) $(FPM) $(GFORTRAN) $(LIBYS-ALIAS)
	PATH="$$PWD/.libys:$$PATH" fpm test --flag "-L.libys"

$(LIBYS-ALIAS): $(LIBYS-SO-FQNP)
	mkdir -p .libys
	cp $(LIBYS-LIB)/libys.dll $@
else
test:: $(LIBYS-SO-FQNP) $(FPM) $(GFORTRAN)
	fpm test --flag "-L$(LIBYS-LIB)"
endif

release:
	$(ROOT)/util/release-fortran

clean::
	$(RM) -r build .libys
