include $(COMMON)/vars-core.mk

CLI_BIN := bin/ys-$(YS_VERSION)
CLI_SRC := \
  src/yamlscript/cli.clj \

CLI_BIN_BASH := bin/ys-sh-$(YS_VERSION)
CLI_BIN_BASH_SRC := share/ys-0.bash

CLI_JAR := \
  target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

CLI_JAR_DEPS := \
  $(LEIN) \
  $(CORE_INSTALLED) \
  $(CLI_SRC) \

CLI_DEPS := \
  $(CLI_BIN) \
  $(CLI_BIN_BASH) \

ifdef YS_NATIVE_BUILD_STATIC
ifeq (true,$(IS_LINUX))
ifeq (true,$(IS_INTEL))
CLI_DEPS := $(MUSL_GCC) $(CLI_DEPS)
NATIVE_OPTS += \
  -H:CCompilerOption=-Wl,-z,stack-size=2097152 \
  --static \
  --libc=musl
endif
endif
endif
