include ../common/base.mk
include $(COMMON)/binding.mk

NODE_INSTALLED := $(shell command -v npm)

NODE_CMDS := \
    test \

#------------------------------------------------------------------------------

ifndef NODE_INSTALLED
$(info *** Warning: npm not found)
endif

test:: update
	NODE_PATH=../nodejs/lib \
	  strace -o strace.txt \
	  node -e 'YS = require("yamlscript"); let ys = new YS(); console.log(ys.load("foo: bar"))'

update:
	npm install

clean::

distclean::
	$(RM) -r node_modules
