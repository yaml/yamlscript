include ../common/base.mk
include $(COMMON)/binding.mk

GO-VERSION := 1.24.4
include $(MAKES)/go.mk
include $(MAKES)/shell.mk


#------------------------------------------------------------------------------
# Go vars:
#------------------------------------------------------------------------------

export CGO_CFLAGS := -I $(ROOT)/libys/lib
export CGO_LDFLAGS := -L $(ROOT)/libys/lib

unexport GOROOT GOPATH GOBIN GOMODCACHE
export GOROOT := $(GO-LOCAL)
export GOPATH := $(ROOT)/go/go
export GOBIN := $(GOROOT)/bin

export PATH := $(GOBIN):$(PATH)


#------------------------------------------------------------------------------
build:: build-doc $(LIBYS-SO-FQNP)

build-doc:: ReadMe.md

test:: build $(GO)
	go $@
	chmod -R u+w $(GOPATH)

release:
	$(ROOT)/util/release-go

clean::
	[[ ! -d $(GOPATH) ]] || \
	  chmod -R u+w $(GOPATH)
	$(RM) -r $(GOPATH)

distclean:: clean
	$(RM) -r $(LOCAL-CACHE)/go*


#------------------------------------------------------------------------------
pseudo-version:
	@TZ=UTC git --no-pager show \
	   --quiet \
	   --abbrev=12 \
	   --date=format-local:%Y%m%d%H%M%S \
	   --format=v0.0.0-%cd-%h
