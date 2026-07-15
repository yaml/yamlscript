MAVEN-CENTRAL-GROUP ?= org.yamlscript
empty :=
space := $(empty) $(empty)
MAVEN-CENTRAL-GROUP-PATH = $(subst .,/,$(MAVEN-CENTRAL-GROUP))
MAVEN-CENTRAL-POM ?= pom.xml
MAVEN-CENTRAL-VERSION ?= $(YAMLSCRIPT_VERSION)
MAVEN-CENTRAL-BUNDLE-ROOT = target/bundle
MAVEN-CENTRAL-BUNDLE-DIR = $(subst $(space),,$(MC-BUNDLE-DIR))
MC-BUNDLE-DIR = $(MAVEN-CENTRAL-BUNDLE-ROOT)/\
  $(MAVEN-CENTRAL-GROUP-PATH)/\
  $(MAVEN-CENTRAL-ARTIFACT)/\
  $(MAVEN-CENTRAL-VERSION)
MAVEN-CENTRAL-BUNDLE-ZIP = target/bundle.zip
MAVEN-CENTRAL-SOURCES-JAR = \
  target/$(MAVEN-CENTRAL-ARTIFACT)-$(MAVEN-CENTRAL-VERSION)-sources.jar
MAVEN-CENTRAL-JAVADOC-JAR = \
  target/$(MAVEN-CENTRAL-ARTIFACT)-$(MAVEN-CENTRAL-VERSION)-javadoc.jar
MAVEN-CENTRAL-JAVADOC-DIR = target/maven-central-javadoc
MAVEN-CENTRAL-UPLOAD-URL = $(subst $(space),,$(MC-UPLOAD-URL))
MC-UPLOAD-URL = https://central.sonatype.com/api/v1/publisher/upload\
  ?publishingType=AUTOMATIC

define ys-secret
$(strip $(shell \
  file=$(1); set -- $(2); \
  for expr; do \
    value=$$(ys -e "$$expr:say" "$$file" 2>/dev/null || true); \
    if [[ -n "$$value" && "$$value" != nil ]]; then \
      printf '%s' "$$value"; \
      break; \
    fi; \
  done \
))
endef

CENTRAL_TOKEN ?= $(call ys-secret,$(HOME)/.yamlscript-secrets.yaml,\
  .scala.token \
  .java.token \
  .central.token)
ifeq (,$(CENTRAL_TOKEN))
CENTRAL_TOKEN := $(call ys-secret,$(HOME)/.yamlstar-secrets.yaml,\
  .scala.token \
  .java.token \
  .central.token)
endif

GPG_KEY_ID ?= $(call ys-secret,$(HOME)/.yamlscript-secrets.yaml,.gpg.key-id)
ifeq (,$(GPG_KEY_ID))
GPG_KEY_ID := $(call ys-secret,$(HOME)/.yamlstar-secrets.yaml,.gpg.key-id)
endif


#------------------------------------------------------------------------------
maven-central-release: maven-central-bundle
ifndef CENTRAL_TOKEN
	$(error Can't determine Maven Central Portal token)
endif
	@echo "Uploading $(MAVEN-CENTRAL-BUNDLE-ZIP) to Maven Central"
	@curl --fail --request POST \
	  --header "Authorization: Bearer $(CENTRAL_TOKEN)" \
	  --form bundle=@$(MAVEN-CENTRAL-BUNDLE-ZIP) \
	  '$(MAVEN-CENTRAL-UPLOAD-URL)'

maven-central-bundle: \
  $(MAVEN-CENTRAL-JAR) \
  $(MAVEN-CENTRAL-POM) \
  $(MAVEN-CENTRAL-SOURCES-JAR) \
  $(MAVEN-CENTRAL-JAVADOC-JAR)
ifndef GPG_KEY_ID
	$(error Can't determine GPG signing key ID)
endif
	$(RM) -r $(MAVEN-CENTRAL-BUNDLE-ROOT) $(MAVEN-CENTRAL-BUNDLE-ZIP)
	mkdir -p $(MAVEN-CENTRAL-BUNDLE-DIR)
	cp $(MAVEN-CENTRAL-JAR) $(MAVEN-CENTRAL-BUNDLE-DIR)/
	cp $(MAVEN-CENTRAL-POM) \
	  $(MAVEN-CENTRAL-BUNDLE-DIR)/\
$(MAVEN-CENTRAL-ARTIFACT)-$(MAVEN-CENTRAL-VERSION).pom
	cp $(MAVEN-CENTRAL-SOURCES-JAR) $(MAVEN-CENTRAL-BUNDLE-DIR)/
	cp $(MAVEN-CENTRAL-JAVADOC-JAR) $(MAVEN-CENTRAL-BUNDLE-DIR)/
	@for f in $(MAVEN-CENTRAL-BUNDLE-DIR)/*.jar \
	    $(MAVEN-CENTRAL-BUNDLE-DIR)/*.pom; do \
	  gpg_args=(--batch --yes -ab -u "$(GPG_KEY_ID)"); \
	  if [[ -n "$${GPG_PASSPHRASE:-}" ]]; then \
	    gpg_args+=(--pinentry-mode loopback --passphrase "$$GPG_PASSPHRASE"); \
	  fi; \
	  gpg "$${gpg_args[@]}" "$$f"; \
	done
	@for f in $(MAVEN-CENTRAL-BUNDLE-DIR)/*.jar \
	    $(MAVEN-CENTRAL-BUNDLE-DIR)/*.pom \
	    $(MAVEN-CENTRAL-BUNDLE-DIR)/*.asc; do \
	  md5sum "$$f" | cut -d' ' -f1 > "$$f.md5"; \
	  sha1sum "$$f" | cut -d' ' -f1 > "$$f.sha1"; \
	done
	cd $(MAVEN-CENTRAL-BUNDLE-ROOT) && zip -r ../bundle.zip .

$(MAVEN-CENTRAL-SOURCES-JAR): $(MAVEN-CENTRAL-SOURCE-FILES)
	mkdir -p target
	jar cf $@ $(MAVEN-CENTRAL-SOURCE-FILES)

$(MAVEN-CENTRAL-JAVADOC-JAR):
	$(RM) -r $(MAVEN-CENTRAL-JAVADOC-DIR)
	mkdir -p $(MAVEN-CENTRAL-JAVADOC-DIR)
	printf '%s\n' "$(MAVEN-CENTRAL-JAVADOC-TEXT)" > \
	  $(MAVEN-CENTRAL-JAVADOC-DIR)/README.txt
	jar cf $@ -C $(MAVEN-CENTRAL-JAVADOC-DIR) .

clean::
	$(RM) -r $(MAVEN-CENTRAL-BUNDLE-ROOT) \
	  $(MAVEN-CENTRAL-BUNDLE-ZIP) \
	  $(MAVEN-CENTRAL-JAVADOC-DIR) \
	  $(MAVEN-CENTRAL-SOURCES-JAR) \
	  $(MAVEN-CENTRAL-JAVADOC-JAR)
