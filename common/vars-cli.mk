include $(COMMON)/vars-core.mk

CLI-BIN := bin/ys-$(YS_VERSION)
CLI-SRC := \
  src/yamlscript/cli.clj \

CLI-BIN-BASH := bin/ys-sh-$(YS_VERSION)
CLI-BIN-BASH-SRC := share/ys-0.bash

CLI-JAR := \
  target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

ifeq (,$(wildcard $(LEIN)))
CLI-JAR-DEPS := $(LEIN)
endif

CLI-JAR-DEPS += \
  $(CORE-INSTALLED) \
  $(CLI-SRC) \

CLI-DEPS := \
  $(CLI-BIN) \
  $(CLI-BIN-BASH) \

ifdef YS_NATIVE_BUILD_STATIC
ifeq (true,$(IS-LINUX))
ifeq (true,$(IS-INTEL))
CLI-DEPS := $(MUSL-GCC) $(CLI-DEPS)
NATIVE-OPTS += \
  -H:CCompilerOption=-Wl,-z,stack-size=2097152 \
  --static \
  --libc=musl
endif
endif
endif
