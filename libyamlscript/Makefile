include ../.common.mk

### For Orable GraalVM No-Fee #################################################
ifndef GRAALVM_CE
GRAALVM_SRC := https://download.oracle.com/graalvm
GRAALVM_VER ?= 21
GRAALVM_TAR := graalvm-jdk-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/latest/$(GRAALVM_TAR)
GRAALVM_PATH ?= /tmp/graalvm-oracle-$(GRAALVM_VER)

### For GraalVM CE (Community Edition) ########################################
else
GRAALVM_SRC := https://github.com/graalvm/graalvm-ce-builds/releases/download
GRAALVM_VER ?= 21
  ifeq (21,$(GRAALVM_VER))
    override GRAALVM_VER := jdk-21.0.0
  endif
  ifeq (17,$(GRAALVM_VER))
    override GRAALVM_VER := jdk-17.0.8
  endif
GRAALVM_TAR := graalvm-community-$(GRAALVM_VER)_$(GRAALVM_ARCH)_bin.tar.gz
GRAALVM_URL := $(GRAALVM_SRC)/$(GRAALVM_VER)/$(GRAALVM_TAR)
GRAALVM_PATH ?= /tmp/graalvm-ce-$(GRAALVM_VER)
endif

GRAALVM_HOME := $(GRAALVM_PATH)$(GRAALVM_SUBDIR)
GRAALVM_DOWNLOAD := /tmp/$(GRAALVM_TAR)

GRAALVM_O ?= 1

BUILD_TARGETS := \
  $(LIBYAMLSCRIPT_SO_PATH) \

export JAVA_HOME := $(GRAALVM_HOME)
export PATH := $(GRAALVM_HOME)/bin:$(PATH)

LIBYAMLSCRIPT_JAR_PATH := target/libyamlscript-0.1.0-standalone.jar
SVM_JAR_PATH := $(GRAALVM_PATH)/lib/svm/builder/svm.jar

YAMLSCRIPT_CORE_INSTALLED := \
  $(HOME)/.m2/repository/yamlscript/core/maven-metadata-local.xml
YAMLSCRIPT_CORE_SRC := ../clojure/src/yamlscript/*

#------------------------------------------------------------------------------
build:: $(BUILD_TARGETS)

graalvm: $(GRAALVM_PATH)

clean::
	$(RM) -r lib/ target/ .lein-* .cpcache/
	$(RM) src/libyamlscript/Core.class

repl-deps:: $(LIBYAMLSCRIPT_JAR_PATH)

distclean::
	# $(RM) $(GRAALVM_DOWNLOAD)
	$(RM) -r $(GRAALVM_PATH)

$(LIBYAMLSCRIPT_SO_PATH): $(LIBYAMLSCRIPT_JAR_PATH) $(GRAALVM_PATH)
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
	time \
	native-image \
	    -O$(GRAALVM_O) \
	    --verbose \
	    --native-image-info \
	    --no-fallback \
	\
	    --initialize-at-build-time \
	    --enable-preview \
	\
	    -H:ReflectionConfigurationFiles=reflection.json \
	    -H:+ReportExceptionStackTraces \
	    -H:IncludeResources=SCI_VERSION \
	    -H:Log=registerResource: \
	    -J-Dclojure.spec.skip-macros=true \
	    -J-Dclojure.compiler.direct-linking=true \
	    -J-Xmx3g \
	\
	    --shared \
	    -jar $(LIBYAMLSCRIPT_JAR_PATH) \
	    -H:Name=$(LIBYAMLSCRIPT_SO_NAME)

$(LIBYAMLSCRIPT_JAR_PATH): $(YAMLSCRIPT_CORE_INSTALLED) $(GRAALVM_PATH)
	lein uberjar

$(YAMLSCRIPT_CORE_INSTALLED): $(YAMLSCRIPT_CORE_SRC)
	$(MAKE) -C ../clojure install

$(GRAALVM_PATH): $(GRAALVM_DOWNLOAD)
	tar xzf $<
	mv graalvm-* $@

$(GRAALVM_DOWNLOAD):
	curl -L -o $@ $(GRAALVM_URL)
