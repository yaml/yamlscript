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

# package.json is tracked and kept current by 'make bump'. In
# installed-artifacts CI mode skip regenerating it: the $(YS) path
# is a unix-style ROOT path that native Windows make cannot stat as
# a prerequisite:
ifdef YS_RELEASE_USE_INSTALLED_YS
update: $(NODE)
else
update: $(NODE) package.json
endif
	npm install

release: $(NODE)
	NODE_AUTH_TOKEN=$(NPM-TOKEN) npm publish --access public

clean::
	$(RM) package-lock.json strace.txt bad

distclean::
	$(RM) -r node_modules .cache

%.json: %.yaml $(YS)
	$(YS) -J $< > $@
