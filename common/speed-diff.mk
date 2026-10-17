include $(MAKES)/python.mk

old ?= HEAD^
new ?= HEAD
cases ?= all
runs ?= 11
warmups ?= 2
case-timeout ?= 60
speed-cache ?= $(ROOT)/.cache/speed-diff
graalvm-bin ?= $(ROOT)/ys/$(CLI-BIN-GRAALVM)

speed-diff: $(PYTHON)
	$(PYTHON) $(ROOT)/util/speed-diff.py \
	  --old '$(old)' --new '$(new)' --cases '$(cases)' \
	  --runs '$(runs)' --warmups '$(warmups)' --cache '$(speed-cache)' \
	  --timeout '$(case-timeout)' \
	  --graalvm-bin '$(graalvm-bin)' \
	  $(if $(glojure-dir),--glojure-dir '$(glojure-dir)') \
	  $(if $(gloat-dir),--gloat-dir '$(gloat-dir)') \
	  $(if $(filter 1 true,$(rebuild)),--rebuild)

speed-diff-test: $(PYTHON)
	$(PYTHON) $(ROOT)/test/speed_diff_test.py
