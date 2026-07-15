include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/dotnet.mk
include $(MAKES)/powershell.mk
include $(MAKES)/shell.mk

export DOTNET_CLI_HOME := $(ROOT)/.cache/dotnet

PSGALLERY-API-KEY ?= $(PSGALLERY_API_KEY)
ifeq (,$(PSGALLERY-API-KEY))
PSGALLERY-API-KEY := \
  $(shell \
    file=~/.yamlscript-secrets.yaml; \
    value=$$(ys -e '.powershell.token:say' $$file 2>/dev/null); \
    [[ "$$value" != nil ]] && printf '%s' "$$value")
endif

PUBLISH-DIR := .publish
MODULE-DIR := $(PUBLISH-DIR)/YAMLScript
MODULE-FILES := YAMLScript.psd1 YAMLScript.psm1 ReadMe.md

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(PWSH)
ifndef PWSH
	$(error 'pwsh' is required to test the PowerShell binding)
endif
	$(PWSH) -NoLogo -NoProfile -File test/test-yamlscript.ps1

release: $(PWSH) $(DOTNET) build-doc
ifndef PWSH
	$(error 'pwsh' is required to publish the PowerShell binding)
endif
ifndef PSGALLERY-API-KEY
	$(error Can't determine PowerShell Gallery API key)
endif
	$(RM) -r $(PUBLISH-DIR)
	mkdir -p $(MODULE-DIR)
	cp $(MODULE-FILES) $(MODULE-DIR)/
	@$(PWSH) -NoLogo -NoProfile -Command \
	  "Publish-Module -Path $(MODULE-DIR) -Repository PSGallery \
	    -NuGetApiKey '$(PSGALLERY-API-KEY)' -Force"

clean::
	$(RM) -r $(PUBLISH-DIR)
