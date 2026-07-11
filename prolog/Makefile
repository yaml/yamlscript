include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/prolog.mk
include $(MAKES)/shell.mk

PROLOG-PRIV := priv
PROLOG-SHIM := $(PROLOG-PRIV)/libyamlscript_prolog.$(SO)

ifeq ($(OS-NAME),macos)
SHIM-LDFLAGS := -dynamiclib
else
SHIM-LDFLAGS := -shared
endif


#------------------------------------------------------------------------------
build:: build-doc $(PROLOG-SHIM)

build-doc:: ReadMe.md

test:: $(TPL)
	@echo "Skipping Prolog tests: Trealla FFI lacks cstring return support"

$(PROLOG-SHIM): c_src/yamlscript_prolog.c | $(PROLOG-PRIV)
	$(CC) -fPIC $(SHIM-LDFLAGS) -o $@ $< -ldl

$(PROLOG-PRIV):
	mkdir -p $@

clean::
	$(RM) -r priv
