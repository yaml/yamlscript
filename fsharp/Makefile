include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/fsharp.mk
include $(MAKES)/shell.mk

PROJECT-FILE := YAMLScript.fsproj
TEST-PROJECT-FILE := test/YAMLScript.Tests.fsproj

export DOTNET_CLI_HOME := $(ROOT)/.cache/dotnet
export NUGET_PACKAGES := $(ROOT)/.cache/nuget/packages


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(DOTNET)
	dotnet run --project $(TEST-PROJECT-FILE)

clean::
	$(RM) -r bin obj test/bin test/obj
