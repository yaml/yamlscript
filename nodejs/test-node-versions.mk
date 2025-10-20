M := .cache/makes
$(shell [ -d $M ] || git clone -q https://github.com/makeplus/makes $M)
include $M/init.mk
include $M/node.mk

export LD_LIBRARY_PATH := ../libys/lib

LIBYS := $(LD_LIBRARY_PATH)/libys.so.0.2.3

NODES := $(shell \
curl -s https://nodejs.org/download/release/ \
  | ys - -e '.split(/href/).filter(/="v/) \
             .map(\(replace(_ /.*v(\d*\.\d+\.\d+).*/ "$$1"))) \
             .mapv(say) && nil' \
  | sort -rV \
  | uniq)

MAKE-THIS := $(MAKE) -f test-node-versions.mk

test: $(NODES)

$(NODES):
	$(MAKE-THIS) test-node NODE-VERSION=$@

test-node: $(NODE) $(LIBYS) node_modules
	@echo '=== $(NODE-VERSION) ==========================================='
	NODE_PATH=lib \
	node -e 'YS = require("yamlscript"); ys = new YS(); console.log(ys.load(fs.readFileSync(0, "utf8")))' \
	<<<$$'!ys-0:\nfoo:: sum(3 .. 9)' || \
	(echo; ( echo 'Bad version: $(NODE-VERSION)' | tee -a bad ); echo)

$(LIBYS):
	$(MAKE) -C ../libys build

node_modules:
	$(MAKE) test NODE-VERSION=24.8.0
