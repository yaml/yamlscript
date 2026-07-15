include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/shell.mk

PWSH ?= pwsh
PSGALLERY-API-KEY ?= $(PSGALLERY_API_KEY)

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP)
ifndef PWSH
	$(error 'pwsh' is required to test the PowerShell binding)
endif
	pwsh -NoLogo -NoProfile -File test/test-yamlscript.ps1

release: build-doc
ifndef PWSH
	$(error 'pwsh' is required to publish the PowerShell binding)
endif
ifndef PSGALLERY-API-KEY
	$(error Can't determine PowerShell Gallery API key)
endif
	pwsh -NoLogo -NoProfile -Command \
	  "Publish-Module -Path . -Repository PSGallery \
	    -NuGetApiKey '$(PSGALLERY-API-KEY)' -Force"

clean::
