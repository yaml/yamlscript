include $(COMMON)/vars-libys.mk

test:: $(LIBYS-SO-FQNP)

$(LIBYS-SO-FQNP): | $(ROOT)/libys
	$(MAKE) -C $(ROOT)/libys build

build-doc:: build-bin

build-bin:
	$(MAKE) -C $(ROOT) build-bin-ys

clean::
	$(RM) *.tmp

ReadMe.md: $(COMMON)/readme.md $(wildcard doc/*.md) $(ROOT)/util/mdys
	mdys $< > $@.tmp
	mv $@.tmp $@
