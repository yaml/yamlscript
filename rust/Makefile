include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/rust.mk
include $(MAKES)/shell.mk

CRATES-TOKEN ?= $(CRATES_TOKEN)
ifeq (,$(CRATES-TOKEN))
ifneq (,$(shell command -v ys))
ifneq (,$(wildcard ~/.yamlscript-secrets.yaml))
CRATES-TOKEN := \
  $(shell ys -e '.crates.token:say' ~/.yamlscript-secrets.yaml 2>/dev/null)
endif
endif
endif

CARGO-CMDS := \
  build \
  check \
  clippy \
  fmt \
  install \
  test \
  update \


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP)

$(CARGO-CMDS):: $(CARGO)
	cargo $@

release: $(CARGO)
	CARGO_REGISTRY_TOKEN=$(CRATES-TOKEN) \
	  cargo publish

clean::
	$(RM) -r target
