include ../common/base.mk
include $(COMMON)/binding.mk
include $(MAKES)/shell.mk

PHP := $(shell command -v php)

build:: build-doc

build-doc:: ReadMe.md

ifdef PHP
$(if $(shell which composer),,$(error PHP composer not installed))

# $(if $(shell which ys),,$(shell $(MAKE) ys &>/dev/null))
# $(error $(shell which ys))

PACKAGIST-USER := \
  $(shell ys -e '.packagist.user:say' ~/.yamlscript-secrets.yaml)
PACKAGIST-TOKEN := \
  $(shell ys -e '.packagist.token:say' ~/.yamlscript-secrets.yaml)
PACKAGIST-URL := \
  https://packagist.org/api/update-package
PACKAGIST-URL := \
  $(PACKAGIST-URL)?username=$(PACKAGIST-USER)&apiToken=$(PACKAGIST-TOKEN)

export PHP_CONF_DIR := $(shell php -i | \
  grep 'Scan this dir' | \
  cut -d '>' -f2 | \
  tr -d ' ' \
)

path:
	@(IFS=:; printf '%s\n' $$PATH)
	which ys

test:: install
	PHP_INI_SCAN_DIR=$(PHP_CONF_DIR) $(PHP) vendor/bin/phpunit

install:: composer.json
	composer $@

release:: $(YS)
ifndef PACKAGIST-TOKEN
	@echo "Error: YAMLScript Packagist credentials not configured"
	@exit 1
endif
	$(ROOT)/util/release-php
	curl -X POST \
	  -H 'content-type:application/json' \
	  '$(PACKAGIST-URL)' \
	  -d '{"repository":{"url":"https://github.com/yaml/yamlscript-php"}}'

endif

clean::
	$(RM) composer.json
	$(RM) composer.lock
	$(RM) .phpunit.result.cache

distclean::
	$(RM) -r vendor

composer.json: composer.yaml $(YS)
	ys -J $< > $@

ys: $(YS)
