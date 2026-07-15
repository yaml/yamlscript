include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/shell.mk

TPL ?= tpl
SWIPL ?= swipl

PROLOG-PRIV := priv
PROLOG-SHIM := $(PROLOG-PRIV)/libyamlscript_prolog.$(SO)
SHIM-CFLAGS := -DYAMLSCRIPT_VERSION=\"$(YAMLSCRIPT_VERSION)\"

ifeq ($(OS-NAME),macos)
SHIM-LDFLAGS := -dynamiclib
else
SHIM-LDFLAGS := -shared
endif


#------------------------------------------------------------------------------
build:: build-doc $(PROLOG-SHIM)

build-doc:: ReadMe.md

test::
ifndef TPL
	$(error 'tpl' is required to test the Prolog binding)
endif
	@echo "Skipping Prolog tests: Trealla FFI lacks cstring return support"

$(PROLOG-SHIM): c_src/yamlscript_prolog.c | $(PROLOG-PRIV)
	$(CC) -fPIC $(SHIM-LDFLAGS) $(SHIM-CFLAGS) -o $@ $< -ldl

$(PROLOG-PRIV):
	mkdir -p $@

clean::
	$(RM) -r priv
