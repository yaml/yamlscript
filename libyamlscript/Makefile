include ../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(COMMON)/vars-core.mk
include $(COMMON)/vars-libys.mk

#------------------------------------------------------------------------------
build:: $(LIBYS_DEPS)

<<<<<<< HEAD
# jar: $(LIBYS_JAR_PATH)
=======
jar: $(LIBYAMLSCRIPT_JAR_PATH) $(RAPIDYAML_INSTALLED)
>>>>>>> 8a8f78d0 (core: Add rapidyaml parser support)

install:: $(LIBYS_DEPS)
	mkdir -p $(PREFIX)/include/
	install -m 644 $(LIBYS_HEADERS) $(PREFIX)/include/
	mkdir -p $(PREFIX)/lib/
	install -m 644 $< $(PREFIX)/lib/
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS_SO_BASE))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS_SO_APIP))
	ln -fs $(notdir $<) $(PREFIX)/lib/$(notdir $(LIBYS_SO_VERS))

realclean::
	$(RM) -r lib/
	$(RM) src/libyamlscript/Core.class

test:: $(LIBYS_SO_FQNP)
	ls -lh lib

repl-deps:: $(LIBYS_JAR_PATH)

$(LIBYS_SO_FQNP): $(LIBYS_JAR_PATH)
ifneq (true,$(LIBZ))
	$(error *** The 'libz.$(SO)' library is required by native-image but not installed)
endif
	mkdir -p $(dir $@)
	# The next command may take a long time (a minute or so)
	$(TIME) \
	native-image \
	  $(NATIVE_OPTS) \
	\
	  --shared \
	  -jar $< \
	  -o $(LIBYS_SO_NAME)
	mv $(LIBYS_SO_BASE) $@
	mv $(LIBYS_SO_NAME).h $(LIBYS_SO_NAME).$(YAMLSCRIPT_VERSION).h
	ln -fs $(notdir $@) $(LIBYS_SO_APIP)
	ln -fs $(notdir $@) $(LIBYS_SO_VERS)
	ln -fs $(notdir $@) $(LIBYS_SO_BASE)
	touch $@

$(LIBYS_JAR_PATH): $(CORE_INSTALLED)
	$(LEIN) uberjar
	touch $@

$(CORE_INSTALLED):
	$(MAKE) -C $(CORE_DIR) $@
