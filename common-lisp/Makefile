include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/shell.mk

LISP-BIN := bin/yamlscript-lisp-json
LISP-CFLAGS := -DYAMLSCRIPT_VERSION=\"$(YAMLSCRIPT_VERSION)\"
SBCL ?= sbcl


#------------------------------------------------------------------------------
build:: build-doc $(LISP-BIN)

build-doc:: ReadMe.md

test::
ifndef SBCL
	$(error 'sbcl' is required to test the Common Lisp binding)
endif
	@echo "Skipping Common Lisp tests: libys crashes from helper process"

$(LISP-BIN): c_src/yamlscript_lisp.c | bin
	$(CC) $(LISP-CFLAGS) -o $@ $< -ldl

bin:
	mkdir -p $@

clean::
	$(RM) -r bin priv
