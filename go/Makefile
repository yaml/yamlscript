include ../common/base.mk
include $(COMMON)/binding.mk


#------------------------------------------------------------------------------
# Go vars:
#------------------------------------------------------------------------------

GO-VER := 1.22.4

ifdef IS-LINUX
  ifdef IS-INTEL
    GO-ARCH := linux-amd64
  else ifdef IS-ARM
    GO-ARCH := linux-arm64
  endif
else ifdef IS-MACOS
  ifdef IS-INTEL
    GO-ARCH := darwin-amd64
  else ifdef IS-ARM
    GO-ARCH := darwin-arm64
  endif
endif

GO-DIR := go$(GO-VER)
GO-TAR := $(GO-DIR).$(GO-ARCH).tar.gz
GO-TARBALL := $(LOCAL-CACHE)/$(GO-TAR)
GO-URL := https://go.dev/dl/$(GO-TAR)
GO-ROOT := $(LOCAL-CACHE)/go$(GO-VER)
GO-BIN := $(GO-ROOT)/bin
GO-INSTALLED := $(GO-BIN)/go

export CGO_CFLAGS := -I $(ROOT)/libys/lib
export CGO_LDFLAGS := -L $(ROOT)/libys/lib

unexport GOROOT GOPATH GOBIN GOMODCACHE
export GOROOT := $(GO-ROOT)
export GOPATH := $(ROOT)/go/go
export GOBIN := $(GOROOT)/bin

export PATH := $(GOBIN):$(PATH)


#------------------------------------------------------------------------------
build:: build-doc $(LIBYS-SO-FQNP) $(GO-INSTALLED)

build-doc:: ReadMe.md

test:: build
	go $@
	chmod -R u+w $(GOPATH)

release:
	$(ROOT)/util/release-go

clean::
	[[ ! -d $(GOPATH) ]] || \
	  chmod -R u+w $(GOPATH)
	$(RM) -r $(GOPATH)

sysclean:: clean
	$(RM) -r $(LOCAL-CACHE)/go*


#------------------------------------------------------------------------------
pseudo-version:
	@TZ=UTC git --no-pager show \
	   --quiet \
	   --abbrev=12 \
	   --date=format-local:%Y%m%d%H%M%S \
	   --format=v0.0.0-%cd-%h

print-goroot: $(GO-INSTALLED)
	@echo $(GOROOT)


#------------------------------------------------------------------------------
$(GO-INSTALLED): $(GO-TARBALL)
	tar xf $< -C $(LOCAL-CACHE)
	mv $(LOCAL-CACHE)/go $(LOCAL-CACHE)/$(GO-DIR)
	touch $@

$(GO-TARBALL):
	$(call need-curl)
	$(CURL) -o $@ $(GO-URL)
