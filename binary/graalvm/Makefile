include ../../common/base.mk
include $(COMMON)/java.mk
include $(COMMON)/clojure.mk
include $(COMMON)/native.mk
include $(MAKES)/shell.mk

JAR := target/uberjar/program-ys-binary-standalone.jar


build:: program

clean::
	$(RM) -r target reports
	$(RM) eval.ys program \
	  program-build-report.html \
		install-core.log \
		native-image.log

realclean::
	$(MAKE) -C $(ROOT)/core realclean
	$(RM) src/program.clj

program: $(JAR)
	native-image \
	  $(NATIVE-OPTIONS) \
	  -jar $< \
	  -o $@ | \
	tee native-image.log | \
	grep -E '^[\#\[]'

$(JAR): src/program.clj
	@echo "* Installing Clojure dependencies..." > /dev/stderr
	@( set -x && \
	   $(MAKE) $(LEIN) && \
	   $(MAKE) -C $(ROOT)/core install && \
	   lein deps; \
	 ) &> install-core.log
	@echo "* Building yamlscript.core jar..." > /dev/stderr
	lein uberjar

src/program.clj:
ifndef YS-PROGRAM-FILE
	@echo 'YS-PROGRAM-FILE make var not set'
	@exit 1
endif
ifndef YS-BIN
	@echo 'YS-BIN make var not set'
	@exit 1
endif
	$(YS-BIN) bin/ys-program-wrapper $(YS-PROGRAM-FILE) > $@
