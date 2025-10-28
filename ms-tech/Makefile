M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
include $M/init.mk
# include $M/ys.mk
include $M/libys.mk
include $M/python.mk
include $M/clean.mk
include $M/shell.mk
SHELL := bash

export YS_UNORDERED=1

PID_FILE := .watcher.pid
LOG_FILE := .watcher.log
PATH := ~/src/yamlscript-vs-rosetta/bin:$(PATH)

override export PATH := $(ROOT)/bin:$(PATH)
export PYTHONPATH := $(ROOT)/yamlscript/python/lib
export LD_LIBRARY_PATH := $(LOCAL-LIB):$(LD_LIBRARY_PATH)

DEPS := \
  $(YS) \
	$(LIBYS) \
	$(PYTHON) \
	yamlscript \

#------------------------------------------------------------------------------
default:: all

all: watch run watch-stop

run: $(DEPS)
	$(MAKE) clean
	clear
ifdef s
	vroom vroom --skip=$s
else
	vroom vroom
endif

compile clean::
	vroom $@

clean:: chart-clean
	$(RM) -r before.yaml after.yaml

chart-clean:
	for i in $$(helm list|awk '{print $$1}'|tail -n +2);do helm uninstall $$i;done
	$(RM) -r chart-* helmys
	$(RM) -r $(HELMYS_OUTPUT) $(HELMYS_THRUPUT) $(HELMYS_INPUT)

realclean:: clean watch-stop
	rm -f $(PID_FILE) $(LOG_FILE)

watch:
ifeq (,$(wildcard $(PID_FILE)))
	( \
	  simple-file-watch --path=slides.vroom --command='make compile' \
	    2>&1 >> $(LOG_FILE) & \
	  echo $$! > $(PID_FILE) \
	)
else
	@echo "Watcher already running: $$(< $(PID_FILE))"
endif

watch-stop:
	-kill -9 $$(< $(PID_FILE))
	rm -f $(PID_FILE)

www-pub:
	make -C ../www realclean publish

info-edit:
	vim ../www/src/info.md

yamlscript:
	git clone https://github.com/yaml/$@
