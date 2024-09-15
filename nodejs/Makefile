include ../common/base.mk
include $(COMMON)/binding.mk

NODE_ROOT := $(ROOT)/nodejs

export NODE_PATH := $(NODE_ROOT)/lib

export NODE_VERSION := v16.20.2

export NVM_DIR := $(NODE_ROOT)/nvm
NVM_PATH := $(NVM_DIR)/versions/node/$(NODE_VERSION)
export NVM_BIN := $(NVM_PATH)/bin
export NVM_INC := $(NVM_PATH)/include/node

NVM_INSTALLER_URL := https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh

export PATH := $(NVM_BIN):$(PATH)

ifdef strace
strace := strace -o strace.txt
endif


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

test:: update $(LIBYAMLSCRIPT_SO_FQNP)
	@printf '%.0s-' {1..80}; echo
	$(strace) node test/test.js

update: $(NVM_DIR)
	npm install

release: $(NVM_DIR)
	npm publish

clean::
	$(RM) package-lock.json strace.txt

distclean::
	$(RM) -r node_modules $(NVM_DIR)

$(NVM_DIR):
	mkdir -p $@
	curl -o- $(NVM_INSTALLER_URL) | PROFILE=/dev/null bash
