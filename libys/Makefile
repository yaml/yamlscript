include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-core.mk
include $(COMMON)/vars-libys.mk
include $(COMMON)/docker.mk

include $(MAKES)/shell.mk

#------------------------------------------------------------------------------
build:: $(LIBYS-DEPS)

# jar: $(LIBYS-JAR-PATH)

install:: $(LIBYS-DEPS)
	mkdir -p $(PREFIX)/include/
	install -m 644 $(LIBYS-HEADERS) $(PREFIX)/include/
	mkdir -p $(PREFIX)/lib/
	install -m 644 $< $(PREFIX)/lib/
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS-SO-BASE))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS-SO-APIP))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS-SO-VERS))

clean::
	$(RM) $(LIBYS-BUILD-LOG)

realclean::
	$(RM) -r lib/
	$(RM) src/libys/Core.class

test:: $(LIBYS-SO-FQNP)

repl-deps:: $(LIBYS-JAR-PATH)

ifndef MAKES-IN-DOCKER
$(LIBYS-SO-FQNP): $(DOCKER-RUN-FILE)
	@$(DOCKER-EXEC) make -C libys $@
else

$(LIBYS-SO-FQNP): $(LIBYS-JAR-PATH) $(REFLECTION-JSON)
ifneq (true,$(LIBZ))
	$(error *** The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
	$(TIME) \
	native-image \
	  $(NATIVE-OPTS) \
	\
	  --shared \
	  -jar $< \
	  -o $(LIBYS-SO-NAME) \
		|& tee $(LIBYS-BUILD-LOG)
	mv $(LIBYS-SO-BASE) $@
	mv $(LIBYS-SO-NAME).h $(LIBYS-SO-NAME).$(YAMLSCRIPT_VERSION).h
	ln -fs $(notdir $@) $(LIBYS-SO-APIP)
	ln -fs $(notdir $@) $(LIBYS-SO-VERS)
	ln -fs $(notdir $@) $(LIBYS-SO-BASE)
	touch $@
endif

$(LIBYS-JAR-PATH): $(CORE-INSTALLED)
	lein uberjar
	touch $@

$(CORE-INSTALLED):
	$(MAKE) -C $(CORE-DIR) $@
