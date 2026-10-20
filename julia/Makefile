include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/julia.mk
include $(MAKES)/shell.mk

export YS
export YS_GH_API_URL := $(YS-GH-API-URL)

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYS-SO-FQNP) deps $(JULIA)
	julia --project=. test/runtests.jl

deps:: $(JULIA)
	julia --project=. -e 'using Pkg; Pkg.add("JSON")'

release: $(YS)
	$(ROOT)/util/release-julia

repl: $(JULIA)
	julia --project=.

clean::
	$(RM) Manifest.toml
