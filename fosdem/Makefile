SHELL := bash

YS_VERSION := 0.1.35

ROOT := $(shell pwd)
INSTALL_DIR := $(ROOT)/install
BIN_DIR := $(INSTALL_DIR)/bin
LIB_DIR := $(INSTALL_DIR)/lib

export PATH := $(BIN_DIR):$(PATH)
export PYTHONPATH := $(LIB_DIR)
export LD_LIBRARY_PATH := $(LIB_DIR)

default:

data-ys:
	ys --load data.ys $n | jq

data-python:
	python3 data.py | jq

dependencies:
	pip3 install yamlscript==$(YS_VERSION) --target $(LIB_DIR)
	curl -s https://yamlscript.org/install | \
	    VERSION=$(YS_VERSION) PREFIX=$(INSTALL_DIR) bash

clean:
	$(RM) -r $(INSTALL_DIR)
