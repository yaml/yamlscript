include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(MAKES)/shell.mk

export CLOJARS_USERNAME ?= $(shell util/get-setting username)
export CLOJARS_PASSWORD ?= $(shell util/get-setting password)

#------------------------------------------------------------------------------

test install deploy:: $(LEIN) $(YAMLSCRIPT-JAVA-INSTALLED)
	lein $@

release: deploy

clean::
	$(RM) pom.xml
