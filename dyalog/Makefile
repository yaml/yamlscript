SHELL := bash
.SHELLFLAGS := -e -o pipefail -c

YAMLSCRIPT_VERSION := 0.2.28
DYALOG-RELEASE-VERSION := $(or $(YS_RELEASE_VERSION_NEW),$n)
IMAGE := yamlscript-dyalog
DOCKER_CONFIG := $(CURDIR)/.cache/docker
DOCKER := DOCKER_CONFIG=$(DOCKER_CONFIG) docker
YS ?= ys
RUN := $(DOCKER) run --rm \
  -v $(CURDIR):/work \
  -w /work \
  -e LD_LIBRARY_PATH=/work/lib \
  --entrypoint sh \
  $(IMAGE) \
  -lc

LIBYS-ARCHIVE := libys-$(YAMLSCRIPT_VERSION)-linux-x64.tar.xz
LIBYS-URL := https://github.com/yaml/yamlscript/releases/download
LIBYS-URL := $(LIBYS-URL)/$(YAMLSCRIPT_VERSION)/$(LIBYS-ARCHIVE)
LIBYS := lib/libys.so.$(YAMLSCRIPT_VERSION)
SHIM := lib/yamlscript_dyalog.so
TATIN-BUILD-DIR := .cache/tatin/build
TATIN-CLIENT := .cache/tatin/tatin-client.json
TATIN-TOKEN-FILE := .cache/tatin/token
TATIN-ZIP := $(TATIN-BUILD-DIR)/yaml-yamlscript-$(DYALOG-RELEASE-VERSION).zip
TATIN-UPLOAD-ID := yaml-yamlscript-$(DYALOG-RELEASE-VERSION)
TATIN-UPLOAD-URL := https://tatin.dev/$(TATIN-UPLOAD-ID)
TATIN-SOURCES := apl-package.json LICENSE src/YAMLScript.apln
TATIN-SOURCES += tools/tatin-release.apls
TATIN-RUN := $(DOCKER) run --rm \
  -v $(CURDIR):/work \
  -v $(CURDIR)/$(TATIN-CLIENT):/home/dyalog/Tatin/tatin-client.json:ro \
  -w /work \
  -e LD_LIBRARY_PATH=/work/lib \
  -e TATIN_BUILD_DIR=/work/$(TATIN-BUILD-DIR) \
  -e TATIN_REGISTRY='[tatin]' \
  -e TATIN_RELEASE_VERSION=$(DYALOG-RELEASE-VERSION) \
  --entrypoint sh \
  $(IMAGE) \
  -lc

.PHONY: FORCE

#------------------------------------------------------------------------------
default:: test

build:: $(SHIM)

build-doc::

test:: build
	$(RUN) 'dyalogscript test/test.apls'

package:: $(TATIN-ZIP)

release: test $(TATIN-ZIP)
ifeq (,$(DYALOG-RELEASE-VERSION))
	@echo "Error: release version not set"
	@exit 1
endif
	curl --fail-with-body -sS -X PUT \
	  -H "api-key: $$(cat $(TATIN-TOKEN-FILE))" \
	  -H "Content-Type: application/octet-stream" \
	  --data-binary @$(TATIN-ZIP) \
	  $(TATIN-UPLOAD-URL)

shell:: build
	$(DOCKER) run --rm -it \
	  -v $(CURDIR):/work \
	  -w /work \
	  -e LD_LIBRARY_PATH=/work/lib \
	  --entrypoint bash \
	  $(IMAGE)

clean::
	$(RM) -r lib .cache

realclean:: clean

distclean:: realclean

FORCE::

$(TATIN-ZIP): FORCE $(TATIN-SOURCES) $(TATIN-CLIENT) | .cache/image
ifeq (,$(DYALOG-RELEASE-VERSION))
	@echo "Error: release version not set"
	@exit 1
endif
	mkdir -p $(TATIN-BUILD-DIR)
	$(TATIN-RUN) 'dyalogscript tools/tatin-release.apls'

$(TATIN-CLIENT): FORCE
	@set -e; \
	mkdir -p "$(@D)"; \
	user="$${TATIN_USER:-$(TATIN_USER)}"; \
	token="$${TATIN_TOKEN:-$(TATIN_TOKEN)}"; \
	user="$${user:-yaml}"; \
	secrets="$${YS_SECRETS_FILE:-}"; \
	if [[ -z "$$secrets" && -r "$$HOME/.yamlscript-secrets" ]]; then \
	  secrets="$$HOME/.yamlscript-secrets"; \
	fi; \
	secrets="$${secrets:-$$HOME/.yamlscript-secrets.yaml}"; \
	if [[ -r "$$secrets" ]]; then \
	  user="$${user:-$$($(YS) -l "$$secrets" -e '.dyalog.user:say')}"; \
	  token="$${token:-$$($(YS) -l "$$secrets" -e '.dyalog.token:say')}"; \
	fi; \
	test "$$user" = yaml || { \
	  echo "Error: Tatin user/group must be yaml"; exit 1; }; \
	test -n "$$token" || { \
	  echo "Error: TATIN_TOKEN not set"; exit 1; }; \
	umask 077; \
	printf '%s\n' "$$token" > $(TATIN-TOKEN-FILE); \
	{ \
	  printf '%s\n' '{'; \
	  printf '%s\n' '  caching: 1,'; \
	  printf '%s\n' '  license: "",'; \
	  printf '%s\n' '  path2cache: "",'; \
	  printf '%s\n' '  registries: ['; \
	  printf '%s\n' '    {'; \
	  printf '%s\n' '      alias: "tatin",'; \
	  printf '      api_key: "%s",\n' "$$token"; \
	  printf '%s\n' '      id: "8e1422b7-deb4-4c51-b65d-45dba54233c3",'; \
	  printf '%s\n' '      noCaching: 0,'; \
	  printf '%s\n' '      port: 0,'; \
	  printf '%s\n' '      priority: 100,'; \
	  printf '%s\n' '      proxy: "",'; \
	  printf '%s\n' '      uri: "https://tatin.dev/",'; \
	  printf '%s\n' '    },'; \
	  printf '%s\n' '    {'; \
	  printf '%s\n' '      alias: "tatin-test",'; \
	  printf '%s\n' '      api_key: "Tatin-Test-API-Key",'; \
	  printf '%s\n' '      id: "c9c0db79-88ba-47c0-ac73-7f7c284d2a9d",'; \
	  printf '%s\n' '      noCaching: 0,'; \
	  printf '%s\n' '      port: 0,'; \
	  printf '%s\n' '      priority: 0,'; \
	  printf '%s\n' '      proxy: "",'; \
	  printf '%s\n' '      uri: "https://test.tatin.dev/",'; \
	  printf '%s\n' '    },'; \
	  printf '%s\n' '  ],'; \
	  printf '%s\n' '  source: "",'; \
	  printf '%s\n' '}'; \
	} > $@

$(SHIM): src/yamlscript_dyalog.c $(LIBYS) | .cache/image
	$(RUN) 'gcc -shared -fPIC -Wall -Wextra -o $@ $< -ldl'

$(LIBYS): .cache/$(LIBYS-ARCHIVE) | .cache/image
	mkdir -p lib .cache/libys
	tar -C .cache/libys --strip-components=1 -xf $<
	cp -pP .cache/libys/libys.so* lib/

.cache/$(LIBYS-ARCHIVE): | .cache/image
	mkdir -p $(@D)
	$(RUN) 'wget -q $(LIBYS-URL) -O $@'

.cache/image: dyalog.dockerfile
	mkdir -p $(DOCKER_CONFIG) $(@D)
	$(DOCKER) build -t $(IMAGE) -f $< .
	touch $@
