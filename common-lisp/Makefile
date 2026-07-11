include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/sbcl.mk
include $(MAKES)/shell.mk

LISP-BIN := bin/yamlscript-lisp-json


#------------------------------------------------------------------------------
build:: build-doc $(LISP-BIN)

build-doc:: ReadMe.md

test:: $(SBCL)
	@echo "Skipping Common Lisp tests: libys crashes from helper process"

$(LISP-BIN): c_src/yamlscript_lisp.c | bin
	$(CC) -o $@ $< -ldl

bin:
	mkdir -p $@

clean::
	$(RM) -r bin priv
