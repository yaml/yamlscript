include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/dotnet.mk
include $(MAKES)/shell.mk

PROJECT-FILE := YAMLScript.fsproj
TEST-PROJECT-FILE := test/YAMLScript.Tests.fsproj
NUGET-SCHEMA := https://api.nuget.org/v3/index.json
NUGET-PACKAGE-DIR := nupkg
NUGET-API-KEY ?= $(NUGET_API_KEY)
ifeq (,$(NUGET-API-KEY))
NUGET-API-KEY := \
  $(shell ys -e '.fsharp.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif

export DOTNET_CLI_HOME := $(ROOT)/.cache/dotnet
export NUGET_PACKAGES := $(ROOT)/.cache/nuget/packages


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(DOTNET)
	dotnet run --project $(TEST-PROJECT-FILE)

release: pack
ifndef NUGET-API-KEY
	$(error Can't determine NuGet API key)
endif
	dotnet nuget push $(NUGET-PACKAGE-DIR)/*.nupkg \
	  --source $(NUGET-SCHEMA) \
	  --api-key $(NUGET-API-KEY) \
	  --skip-duplicate

pack: $(DOTNET)
	dotnet pack $(PROJECT-FILE) \
	  --configuration Release \
	  --output $(NUGET-PACKAGE-DIR) \
	  --verbosity normal

clean::
	$(RM) -r bin obj test/bin test/obj
	$(RM) -r $(NUGET-PACKAGE-DIR)
