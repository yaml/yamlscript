include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/r.mk
include $(MAKES)/shell.mk

RLIB := .rlib


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

# Use R (not Rscript): the relocated Rscript binary has the build
# prefix compiled in and cannot find its R home. Avoid --vanilla,
# which would skip the Rprofile.site relocation shim from r.mk:
R-RUN := R --no-save --no-restore --no-init-file --no-echo

test:: $(LIBYS-SO-FQNP) $(R)
	mkdir -p $(RLIB)
	R_LIBS="$(RLIB)" $(R-RUN) -f tests/install-deps.R
	R CMD INSTALL --library=$(RLIB) .
	R_LIBS="$(RLIB)" $(R-RUN) -f tests/test-yamlscript.R

release:
	$(ROOT)/util/release-r

clean::
	$(RM) -r $(RLIB) src/*.o src/*.so
