include $(COMMON)/vars-core.mk

ifneq (,$(filter wasip1/wasm,$(GLOAT_PLATFORM)))
CLI-BIN-GLOJURE := bin/ys-$(YS_VERSION).wasm
else ifneq (,$(filter windows/%,$(GLOAT_PLATFORM)))
CLI-BIN-GLOJURE := bin/ys-$(YS_VERSION).exe
else ifeq ($(OS-NAME),windows)
CLI-BIN-GLOJURE := bin/ys-$(YS_VERSION).exe
CLI-BIN-GRAALVM := bin/ys-$(YS_VERSION)-graalvm.exe
else
CLI-BIN-GLOJURE := bin/ys-$(YS_VERSION)
endif
CLI-BIN-GRAALVM ?= bin/ys-$(YS_VERSION)-graalvm
CLI-BIN := $(if $(filter graalvm,$(YAMLSCRIPT_ENGINE)),\
  $(CLI-BIN-GRAALVM),$(CLI-BIN-GLOJURE))
CLI-OUT := $(CLI-BIN)
CLI-SRC := \
  src/yamlscript/cli.clj \
  src/yamlscript/util/install.clj \
  src/yamlscript/util_platform.clj \


CLI-JAR := \
  target/uberjar/yamlscript.cli-$(YS_VERSION)-SNAPSHOT-standalone.jar

ifeq (,$(wildcard $(LEIN)))
CLI-JAR-DEPS := $(LEIN)
endif

CLI-JAR-DEPS += \
  $(CORE-INSTALLED) \
  $(CLI-SRC) \

CLI-DEPS := $(CLI-BIN)

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

YS-BUILD-LOG := $(ROOT)/build-ys.log
