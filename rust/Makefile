include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/rust.mk
include $(MAKES)/shell.mk

ifneq (,$(shell command -v ys))
ifneq (,$(wildcard ~/.yamlscript-secrets.yaml))
CRATES-TOKEN := \
  $(shell ys -e '.crates.token:say' ~/.yamlscript-secrets.yaml)
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

release:
	CARGO_REGISTRY_TOKEN=$(CRATES-TOKEN) \
	  cargo publish

clean::
	$(RM) -r target
