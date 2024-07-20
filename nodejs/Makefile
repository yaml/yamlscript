include ../common/base.mk
include $(COMMON)/binding.mk

ifeq (,$(shell command -v node))
$(info *** Warning: node not found)
else ifeq (,$(shell command -v npm))
$(info *** Warning: npm not found)
endif

NODEJS := $(shell command -v node)
NPM := $(shell command -v npm)

NODE_CMDS := \
    test \

export NODE_PATH := $(ROOT)/nodejs/lib

ifdef strace
strace := strace -o strace.txt
endif

# export LIBYAMLSCRIPT_DEBUG := 1


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef NODEJS
ifdef NPM
test:: update
	make -C ../libyamlscript $(LIBYAMLSCRIPT_SO_FQNP)
	@printf '%.0s-' {1..80}; echo
	$(strace) node test/test.js

update:
	$(NPM) install

release:
	$(NPM) publish
endif
endif

clean::
	$(RM) package-lock.json strace.txt

distclean::
	$(RM) -r node_modules
