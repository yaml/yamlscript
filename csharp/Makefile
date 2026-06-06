include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/dotnet.mk
include $(MAKES)/shell.mk

PROJECT-FILE := YAMLScript.csproj
TEST-PROJECT-FILE := test/YAMLScript.Tests.csproj

NUGET-SCHEMA := https://api.nuget.org/v3/index.json
NUGET-PACKAGE-DIR := nupkg
NUGET-API-KEY ?= $(NUGET_API_KEY)
ifeq (,$(NUGET-API-KEY))
NUGET-API-KEY := \
  $(shell ys -e '.nuget.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif

build:: build-doc build-project

build-doc:: ReadMe.md

build-project:: $(DOTNET) update
	dotnet build $(PROJECT-FILE) /p:GenerateAssemblyInfo=false

test:: $(DOTNET)
	dotnet test $(TEST-PROJECT-FILE)

release: pack
ifndef NUGET-API-KEY
	$(error Can't determine NuGet API key)
endif
	dotnet nuget push $(NUGET-PACKAGE-DIR)/*.nupkg \
		--source $(NUGET-SCHEMA) \
		--api-key $(NUGET-API-KEY)

pack: $(DOTNET) update
	dotnet pack $(PROJECT-FILE) \
		--configuration Release \
		--output $(NUGET-PACKAGE-DIR) \
		--verbosity normal

pack-test: pack
	dotnet nuget locals all --clear
	dotnet add package $(NUGET-PACKAGE-DIR)/*.nupkg \
	  --source $(NUGET-PACKAGE-DIR)

update: $(PROJECT-FILE)
	perl -pi \
	  -e 's/(Version>)\d+\.\d+\.\d+</$${1}$(YAMLSCRIPT_VERSION)</' $<

clean::
	$(RM) -r bin/ obj/ test/bin/ test/obj/
	$(RM) -r $(NUGET-PACKAGE-DIR)/

realclean:: clean

distclean:: realclean
