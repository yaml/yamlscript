include ../common/base.mk
include $(COMMON)/binding.mk

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYAMLSCRIPT_SO_FQNP) deps
	julia --project=. test/runtests.jl

deps::
	julia --project=. -e 'using Pkg; Pkg.add("JSON")'

release:
	@echo '>>>>>>>> AUTOMATE ME <<<<<<<<'

repl:
	julia --project=.

clean::
	$(RM) Manifest.toml
