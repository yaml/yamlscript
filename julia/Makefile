include ../common/base.mk
include $(COMMON)/binding.mk

HEAD_COMMIT := $(shell git rev-parse HEAD)

#------------------------------------------------------------------------------
build:: build-doc

build-doc:: ReadMe.md

test:: $(LIBYAMLSCRIPT_SO_FQNP) deps
	julia --project=. test/runtests.jl

deps::
	julia --project=. -e 'using Pkg; Pkg.add("JSON")'

release:
ifndef GH_TOKEN
	$(error GH_TOKEN not found)
endif
	curl -L -X POST \
	  -H "Accept: application/vnd.github+json" \
	  -H "Authorization: Bearer $(GH_TOKEN)" \
	  -H "X-GitHub-Api-Version: 2022-11-28" \
	  $(YS_GH_API_URL)/commits/$(HEAD_COMMIT)/comments \
	  -d '{"body":"@JuliaRegistrator register\n"}'

repl:
	julia --project=.

clean::
	$(RM) Manifest.toml
