include $(COMMON)/vars-libys.mk

test:: $(LIBYS-SO-FQNP)

$(LIBYS-SO-FQNP):
	$(MAKE) -C $(ROOT)/libys build

build-doc:: $(YS)

clean::
	$(RM) *.tmp

ReadMe.md: $(COMMON)/readme.md $(wildcard doc/*.md) $(ROOT)/util/mdys
	mdys $< > $@.tmp
	mv $@.tmp $@
