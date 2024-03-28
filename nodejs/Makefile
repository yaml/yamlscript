include ../common/base.mk
include $(COMMON)/binding.mk

ifeq (,$(shell command -v node))
$(info *** Warning: node not found)
endif
ifeq (,$(shell command -v npm))
$(info *** Warning: npm not found)
endif

NODE_CMDS := \
    test \

export NODE_PATH := $(ROOT)/nodejs/lib

ifdef strace
strace := strace -o strace.txt
endif

export LIBYAMLSCRIPT_DEBUG := 1

#------------------------------------------------------------------------------

test:: update
	make -C ../libyamlscript $(LIBYAMLSCRIPT_SO_FQNP)
	@printf '%.0s-' {1..80}; echo
	$(strace) node test.js

update:
	npm install

clean::

distclean::
	$(RM) -r node_modules
