include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/powershell.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(PWSH)
	pwsh -NoLogo -NoProfile -File test/test-yamlscript.ps1

clean::
