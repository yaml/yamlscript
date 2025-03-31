SHELL := bash

ROOT := $(shell \
  cd '$(abspath $(dir $(lastword $(MAKEFILE_LIST))))' && pwd -P)

PID_FILE := .watcher.pid
LOG_FILE := .watcher.log

export PATH := $(ROOT)/bin:$(PATH)

#------------------------------------------------------------------------------
default: run

all: watch default watch-stop

run:
ifdef s
	vroom vroom --skip=$s
else
	vroom vroom
endif

compile clean:
	vroom $@

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
