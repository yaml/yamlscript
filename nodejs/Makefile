include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/node.mk

NODE-ROOT := $(ROOT)/nodejs
export NODE_PATH := $(NODE-ROOT)/lib
export NODE_VERSION := $(NODE-VERSION)

ifdef strace
strace := strace -o strace.txt
endif


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef NODE
test:: update $(LIBYS-SO-FQNP)
	which -a node
	node --version
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
	$(RM) -r node_modules $(NODE)
