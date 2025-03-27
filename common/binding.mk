include $(COMMON)/vars-rapidyaml.mk
include $(COMMON)/vars-libys.mk

test:: $(LIBYS_SO_FQNP)

$(LIBYS_SO_FQNP): | $(ROOT)/libyamlscript
	$(MAKE) -C $(ROOT)/libyamlscript build

build-doc:: build-bin

build-bin:
	$(MAKE) -C $(ROOT) build-bin-ys

clean::
	$(RM) *.tmp

ReadMe.md: $(COMMON)/readme.md $(wildcard doc/*.md) $(ROOT)/util/mdys
	mdys $< > $@.tmp
	mv $@.tmp $@
