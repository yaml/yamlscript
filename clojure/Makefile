include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/clojure.mk

export CLOJARS_USERNAME ?= $(shell util/get-setting username)
export CLOJARS_PASSWORD ?= $(shell util/get-setting password)

#------------------------------------------------------------------------------

test install deploy:: $(LEIN) $(YAMLSCRIPT_JAVA_INSTALLED)
	$< $@

release: deploy
