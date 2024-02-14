include ../common/base.mk
include $(COMMON)/binding.mk
include $(COMMON)/clojure.mk

MVN_COMMANDS := \
  clean \
	deploy \
	install \
	package \
	test \

#------------------------------------------------------------------------------

$(MVN_COMMANDS):: $(JAVA_INSTALLED)
	mvn $@
