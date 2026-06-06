include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/lua.mk
include $(MAKES)/luarocks.mk
include $(MAKES)/python.mk
include $(MAKES)/shell.mk

LUA-RELEASE-REVISION ?= 1
LUA-API-VERSION := \
  $(word 1,$(subst ., ,$(LUA-VERSION))).$(word 2,$(subst ., ,$(LUA-VERSION)))

export LUA_PATH := $(ROOT)/lua/lib/?.lua;;
export LUA_CPATH := $(LOCAL-PREFIX)/lib/lua/$(LUA-API-VERSION)/?.so;;

LUA-CFFI := $(LOCAL-LIB)/lua/$(LUA-API-VERSION)/cffi.so
LUA-CJSON := $(LOCAL-LIB)/lua/$(LUA-API-VERSION)/cjson.so
LUA-MESON := $(PYTHON-VENV)/bin/meson

LUA-DEPS := \
  $(LUA) \
  $(LUAROCKS) \
  $(LIBYS-SO-FQNP) \
  $(LUA-CFFI) \
  $(LUA-CJSON) \

LUA-ROCKSPEC := \
  yamlscript-$(YAMLSCRIPT_VERSION)-$(LUA-RELEASE-REVISION).rockspec

LUAROCKS-API-KEY ?= $(LUAROCKS_API_KEY)
ifeq (,$(LUAROCKS-API-KEY))
LUAROCKS-API-KEY := \
  $(shell ys -e '.luarocks.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif


build:: build-doc

build-doc:: ReadMe.md

ifdef IS-MACOS
test::
	@echo "Skipping Lua tests on macOS: cffi-lua fails on macOS ARM64"
else
test:: test-basic test-ffi
endif

test-basic: $(LUA-DEPS)
	lua test/test_basic.lua

lua-test-1 := \
local ys = require("yamlscript"); \
local data = ys.new():load("inc: 41"); \
print(data)

lua-test-2 := \
local ys = require("yamlscript"); \
local data = ys.new():load("!ys-0\ninc: 41"); \
print(data)

test-ffi: $(LUA-DEPS)
	lua -e '$(lua-test-1)'
	lua -e '$(lua-test-2)'

release: $(LUA-ROCKSPEC) $(YS)
ifndef YS_RELEASE_VERSION_NEW
	@echo "Error: YS_RELEASE_VERSION_NEW not set"
	@exit 1
endif
ifndef LUAROCKS-API-KEY
	@echo "Error: YAMLScript LuaRocks credentials not configured"
	@exit 1
endif
	$(ROOT)/util/release-lua
	luarocks upload --api-key '$(LUAROCKS-API-KEY)' $(LUA-ROCKSPEC)

clean::
	$(RM) yamlscript-*.rock
	$(RM) yamlscript-*.rockspec
	$(RM) yamlscript-*.tar.gz

$(LUA-ROCKSPEC): yamlscript.rockspec
	perl -pe 's/%VERSION%/$(YAMLSCRIPT_VERSION)/g; \
	          s/%REVISION%/$(LUA-RELEASE-REVISION)/g;' $< > $@

$(LUA-CFFI): $(LUAROCKS) $(LUA-MESON)
	luarocks install cffi-lua MESON_BINDIR=$(PYTHON-VENV)/bin

$(LUA-CJSON): $(LUAROCKS) $(LUA-MESON)
	luarocks install lua-cjson MESON_BINDIR=$(PYTHON-VENV)/bin

$(LUA-MESON): $(PYTHON-VENV)
	uv pip install --python $(PYTHON-VENV) meson ninja
