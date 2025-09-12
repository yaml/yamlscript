include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/node.mk
include $(MAKES)/shell.mk

NODE-ROOT := $(ROOT)/nodejs
export NODE_PATH := $(NODE-ROOT)/lib

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

test:: update $(LIBYS-SO-FQNP) $(NODE)
	@printf '%.0s-' {1..80}; echo
	$(strace) node test/test.js

update: $(NODE)
	npm install

release: $(NODE)
	npm publish

clean::
	$(RM) package-lock.json strace.txt

distclean::
	$(RM) -r node_modules
