include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/node.mk
include $(MAKES)/shell.mk

NODE-ROOT := $(ROOT)/nodejs
export NODE_PATH := $(NODE-ROOT)/lib

NPM-TOKEN ?= $(NODE_AUTH_TOKEN)
ifeq (,$(NPM-TOKEN))
NPM-TOKEN := $(NPM_TOKEN)
endif
ifeq (,$(NPM-TOKEN))
NPM-TOKEN := \
  $(shell ys -e '.npm.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif

#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

test:: update $(LIBYS-SO-FQNP) $(NODE)
	@printf '%.0s-' {1..80}; echo
	$(strace) node test/test.js

update: $(NODE) package.json
	npm install

release: $(NODE)
	NODE_AUTH_TOKEN=$(NPM-TOKEN) npm publish

clean::
	$(RM) package-lock.json strace.txt bad

distclean::
	$(RM) -r node_modules .cache

%.json: %.yaml $(YS)
	$(YS) -J $< > $@
