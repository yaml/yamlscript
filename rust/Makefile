include ../common/base.mk
include $(COMMON)/binding.mk

CARGO-HOME := $(LOCAL-ROOT)/cargo
export CARGO_HOME := $(CARGO-HOME)
RUSTUP-HOME := $(LOCAL-ROOT)/rustup
export RUSTUP_HOME := $(RUSTUP-HOME)

CARGO-BIN := $(CARGO-HOME)/bin

override PATH := $(CARGO-BIN):$(PATH)
export PATH

CARGO := $(CARGO-BIN)/cargo
RUSTUP := $(CARGO-BIN)/rustup

CARGO-CMDS := \
  build \
  check \
  clippy \
  fmt \
  install \
  publish \
  test \
  update \


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP)

$(CARGO-CMDS):: $(CARGO)
	cargo $@

release: publish

clean::
	$(RM) -r target

distclean::
	$(RM) -r $(CARGO-HOME) $(RUSTUP-HOME)

$(CARGO):
	@echo "Installing '$@'"
	curl --proto '=https' --tlsv1.2 -sSf \
	  https://sh.rustup.rs | \
	  RUSTUP_HOME=$(RUSTUP-HOME) \
	  CARGO_HOME=$(CARGO-HOME) \
	  RUSTUP_INIT_SKIP_PATH_CHECK=yes \
	  bash -s -- \
	    -q -y \
	    --profile minimal \
	    --no-modify-path \
	> /dev/null
	rustup component add clippy
	rustup component add rustfmt
	touch $@
