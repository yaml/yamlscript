include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/gnat.mk
include $(MAKES)/alire.mk
include $(MAKES)/shell.mk

ADA-ORIGIN := https://github.com/yaml/yamlscript.git
ADA-MANIFEST := ada/alire.toml
ADA-RELEASE-COMMIT ?= $(shell git rev-parse HEAD)
ADA-GITHUB-USER ?= \
  $(shell ys -e '.github.user:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
ADA-GITHUB-TOKEN ?= \
  $(shell ys -e '.github.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)

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

release: $(ALR) test build-doc
ifndef ALR
	$(error 'alr' is required to publish the Ada binding)
endif
ifndef n
	$(error 'make $@' needs the n variable set to the new version)
endif
ifndef ADA-GITHUB-USER
	$(error Can't determine GitHub user)
endif
ifndef ADA-GITHUB-TOKEN
	$(error Can't determine GitHub token)
endif
	mkdir -p $(XDG_RUNTIME_DIR)
	$(ALR) settings --global --set user.github_login $(ADA-GITHUB-USER)
	@GH_TOKEN=$(ADA-GITHUB-TOKEN) \
	  $(ALR) publish --manifest $(ADA-MANIFEST) \
	    $(ADA-ORIGIN) $(ADA-RELEASE-COMMIT)

obj:
	mkdir -p $@

clean::
	$(RM) -r obj test/test_yamlscript *.ali *.o
