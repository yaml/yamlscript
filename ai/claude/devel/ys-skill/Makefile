M := ../../../../.cache/makes
$(shell [ -d $M ] || (git clone -q https://github.com/makeplus/makes $M))

include $M/init.mk
include $M/ys.mk
include $M/clean.mk
include $M/shell.mk

MAKES-CLEAN := \
  sessions/a.ys \

override export PATH := $(ROOT)/bin:$(PATH)


check: $(YS)
	skill-audit

PLUGIN_JSON := ../../ys-skill/.claude-plugin/plugin.json
MARKET_JSON := ../../../../.claude-plugin/marketplace.json

bump:
	@old=$$(grep -o '"version": "[^"]*"' $(PLUGIN_JSON) | grep -o '[0-9]*"$$' | tr -d '"'); \
	new=$$((old + 1)); \
	sed -i "s/\(\"version\": \"[0-9]*\.[0-9]*\.\)[0-9]*/\1$$new/" \
	  $(PLUGIN_JSON) $(MARKET_JSON); \
	echo "Bumped to version $$(grep -o '"version": "[^"]*"' $(PLUGIN_JSON) | grep -o '[0-9.]*[0-9]')"
