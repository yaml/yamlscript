SHELL := bash

ROOT := $(shell \
  cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

PID_FILE := .watcher.pid
LOG_FILE := .watcher.log
PATH := ~/src/yamlscript-vs-rosetta/bin:$(PATH)

export PATH := $(ROOT)/bin:$(PATH)
export PYTHONPATH := $(ROOT)/../python/lib
export HELMYS_INPUT := $(PWD)/helmys-input.yaml
export HELMYS_THRUPUT := $(PWD)/helmys-thruput.yaml
export HELMYS_OUTPUT := $(PWD)/helmys-output.yaml

#------------------------------------------------------------------------------
default: run

all: watch default watch-stop

run: clean
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

realclean: clean watch-stop
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
