include ../common/base.mk
include $(COMMON)/binding.mk

ifeq ($(OS-NAME),windows)
# The Windows CI runners have no system php or composer; use the
# makes php (with ffi enabled) and a downloaded composer.phar:
include $(MAKES)/php.mk
COMPOSER-PHAR := $(LOCAL-CACHE)/composer.phar
COMPOSER := $(PHP) $(COMPOSER-PHAR)
COMPOSER-DEPS := $(PHP) $(COMPOSER-PHAR)
# No bare 'ys' on Windows; the artifacts install ys-<version>.exe:
YS-RUN := $(YS)
else
# include $(MAKES)/php.mk  # TODO
PHP := $(shell command -v php)
COMPOSER := composer
COMPOSER-DEPS :=
YS-RUN := ys
endif

include $(MAKES)/shell.mk

build:: build-doc

build-doc:: ReadMe.md

ifdef PHP
ifneq ($(OS-NAME),windows)
$(if $(shell which composer),,$(error PHP composer not installed))
endif

# $(if $(shell which ys),,$(shell $(MAKE) ys &>/dev/null))
# $(error $(shell which ys))

PACKAGIST-USER ?= $(PACKAGIST_USER)
PACKAGIST-TOKEN ?= $(PACKAGIST_TOKEN)
ifeq (,$(PACKAGIST-USER))
PACKAGIST-USER := \
  $(shell ys -e '.packagist.user:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif
ifeq (,$(PACKAGIST-TOKEN))
PACKAGIST-TOKEN := \
  $(shell ys -e '.packagist.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif
PACKAGIST-URL := \
  https://packagist.org/api/update-package
PACKAGIST-URL := \
  $(PACKAGIST-URL)?username=$(PACKAGIST-USER)&apiToken=$(PACKAGIST-TOKEN)

# The makes php on Windows loads the generated php.ini next to
# php.exe instead of a scan dir:
ifneq ($(OS-NAME),windows)
export PHP_CONF_DIR := $(shell php -i | \
  grep 'Scan this dir' | \
  cut -d '>' -f2 | \
  tr -d ' ' \
)
endif

path:
	@(IFS=:; printf '%s\n' $$PATH)
	which ys

test:: install
	PHP_INI_SCAN_DIR=$(PHP_CONF_DIR) $(PHP) vendor/bin/phpunit

# composer.json is tracked and kept current by 'make bump'. In
# installed-artifacts CI mode skip regenerating it: ys.exe writes
# the platform encoding on Windows, which breaks composer's UTF-8
# parse (and native Windows make cannot stat the ys path anyway):
ifdef YS_RELEASE_USE_INSTALLED_YS
install:: $(COMPOSER-DEPS)
else
install:: composer.json $(COMPOSER-DEPS)
endif
	$(COMPOSER) install

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
	$(RM) composer.lock
	$(RM) .phpunit.result.cache

distclean::
	$(RM) -r vendor

composer.json: composer.yaml $(YS)
	$(YS-RUN) -J $< > $@

ifeq ($(OS-NAME),windows)
$(COMPOSER-PHAR):
	@echo "* Installing 'composer' locally"
	curl+ https://getcomposer.org/download/latest-stable/composer.phar \
	  > $@
endif

ys: $(YS)
