PYTHON := $(shell command -v python3)
PYTHON ?= $(shell command -v python)

ifeq (,$(shell which pip || which pip3))
PYTHON :=
endif
