include ../common/base.mk
include $(COMMON)/binding.mk
NODE-VERSION := 16.20.2
include $(MAKES)/node.mk
include $(MAKES)/shell.mk

NODE-ROOT := $(ROOT)/nodejs
export NODE_PATH := $(NODE-ROOT)/lib

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef NODE
test:: update $(LIBYS-SO-FQNP) $(NODE)
	@printf '%.0s-' {1..80}; echo
	$(strace) node test/test.js

update: $(NODE)
	npm install

release: $(NODE)
	npm publish
endif

clean::
	$(RM) package-lock.json strace.txt

distclean::
	$(RM) -r node_modules
