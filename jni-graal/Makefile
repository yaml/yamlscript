include ../common/base.mk
# This sets up JAVA_HOME and puts java and javac in the PATH:
include $(COMMON)/java.mk
include common.mk

export PATH := $(PWD):$(PATH)

CSRC := c
CPPSRC := cpp
JAVASRC := src

TEST_DEPS := \
  $(BINNAME) \
  $(CSRC)/lib$(SONAME).so \
  $(CPPSRC)/lib$(SONAME).so \

G := \e[32m
Z := \e[0m


#------------------------------------------------------------------------------
default::

test: $(TEST_DEPS) test-native-to-c test-native-to-cpp test-jar-to-c
	@echo -e "$GIt worked!!!$Z"

test-native-to-c: $(CSRC)/lib$(SONAME).so $(BINNAME)
	@echo '*** Testing $@'
	LD_LIBRARY_PATH=$(CSRC) $(BINNAME)
	@echo

test-native-to-cpp: $(CPPSRC)/lib$(SONAME).so $(BINNAME)
	@echo '*** Testing $@'
	LD_LIBRARY_PATH=$(CPPSRC) $(BINNAME)
	@echo

test-jar-to-c: $(CSRC)/lib$(SONAME).so $(JAVASRC)/$(LIBNAME).jar
	@echo '*** Testing $@'
	LD_LIBRARY_PATH=$(CSRC) java -jar $(JAVASRC)/$(LIBNAME).jar
	@echo

clean::
	$(RM) $(BINNAME)
	$(RM) -r reports
	$(MAKE) -C $(CSRC) $@
	$(MAKE) -C $(CPPSRC) $@
	$(MAKE) -C $(JAVASRC) $@

status:
	git status --ignored


#------------------------------------------------------------------------------
$(CSRC)/lib$(SONAME).so:
	make -C $(CSRC) $(@:$(CSRC)/%=%)

$(CPPSRC)/lib$(SONAME).so:
	make -C $(CPPSRC) $(@:$(CPPSRC)/%=%)

$(BINNAME): $(JAVASRC)/$(LIBNAME).jar
	native-image -jar $< -H:Name=$@ \
	  -H:ConfigurationFileDirectories=config \
	  "-J-Xmx1g" \
	  --verbose \
	  --no-fallback \
	  --no-server \
	  --initialize-at-build-time \
	  -H:+PrintClassInitialization \
	  -H:+ReportExceptionStackTraces

$(JAVASRC)/$(LIBNAME).jar:
	$(MAKE) -C $(JAVASRC) $(@:$(JAVASRC)/%=%)
