include ../.common.mk

export PATH := $(ROOT)/clojure/bin:$(PATH)

DOCKER_IMAGE := yamlscript-clojure:latest
DOCKER_HISTORY := /tmp/yamlscript-clojure-bash-history

#------------------------------------------------------------------------------
clean:: nrepl-stop
	$(RM) .lein-failures .lein-repl-history pom.xml
	$(RM) -r .calva/ .clj-kondo/ .lsp/ .vscode/ .cpcache/ .portal/
	$(RM) -r target/

test:
	lein test

repl::
ifneq (,$(wildcard .nrepl-pid))
	@echo "Connecting to nREPL server on port $$(< .nrepl-port)"
	lein repl :connect
endif

nrepl: .nrepl-pid
	@echo "nREPL server running on port $$(< .nrepl-port)"

nrepl-stop:
ifneq (,$(wildcard .nrepl-pid))
	@( \
	  pid=$$(< .nrepl-pid); \
	  read -r pid1 <<<"$$(ps --ppid $$pid -o pid=)"; \
	  read -r pid2 <<<"$$(ps --ppid $$pid1 -o pid=)"; \
	  set -x; \
	  kill -9 $$pid $$pid1 $$pid2; \
	)
	$(RM) .nrepl-*
endif

.nrepl-pid:
	( \
	  lein repl :headless & \
	  echo $$! > $@ \
	)
	@( \
	  while [[ ! -f .nrepl-port ]]; do \
	    sleep 0.3; \
	  done; \
	)

docker-build:
	docker build --tag $(DOCKER_IMAGE) .

docker-shell: docker-build
	touch $(DOCKER_HISTORY)
	docker run --rm -it \
	    --volume $(ROOT):/host \
	    --volume $(DOCKER_HISTORY):/root/.bash_history \
	    --workdir /host/clojure \
	    --entrypoint /bin/bash \
	    $(DOCKER_IMAGE)
