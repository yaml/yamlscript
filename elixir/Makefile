include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/elixir.mk
include $(MAKES)/shell.mk

export MIX_HOME := $(ROOT)/.cache/mix
export HEX_HOME := $(ROOT)/.cache/hex


#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) $(ERL) $(ELIXIR)
	mix local.hex --force --if-missing
	mix deps.get
	mix test

# Hex requires the file names README.md and a LICENSE:
release: $(ERL) $(ELIXIR) README.md
	mix local.hex --force --if-missing
	mix deps.get
	mix hex.publish --yes
	$(RM) README.md

README.md: ReadMe.md
	cp $< $@

clean::
	$(RM) -r _build deps priv README.md
