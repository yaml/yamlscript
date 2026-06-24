include $(COMMON)/vars-libys.mk

test:: $(LIBYS-SO-FQNP)

$(LIBYS-SO-FQNP):
ifdef YS_RELEASE_USE_INSTALLED_LIBYS
	test -f $@
else
	$(MAKE) -C $(ROOT)/libys build
endif

build-doc:: $(YS)

clean::
	$(RM) *.tmp

ReadMe.md: $(COMMON)/readme.md $(wildcard doc/*.md) $(ROOT-MAKE)/util/mdys
	mdys $< > $@.tmp
	mv $@.tmp $@
