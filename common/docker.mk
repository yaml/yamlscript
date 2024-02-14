DOCKER_NAME := yamlscript-$(SUBDIR)
DOCKER_IMAGE := $(DOCKER_NAME):latest
DOCKER_HISTORY := $(YS_TMP)/$(DOCKER_NAME)-bash-history
export DOCKER_UID := $(shell id -u)
export DOCKER_GID := $(shell id -g)

ifneq (,$(wildcard /.dockerenv))
  DOCKERENV := 1
endif

docker-build:: Dockerfile
	cp $(COMMON)/project.clj .project.clj
	docker build \
	    --build-arg UBUNTU_VERSION=22.04 \
	    --build-arg DOCKER_UID=$(DOCKER_UID) \
	    --build-arg DOCKER_GID=$(DOCKER_GID) \
	    --tag $(DOCKER_IMAGE) .
	$(RM) $< .project.clj

docker-test:: docker-build
	docker run --rm -it \
	    --volume $(ROOT):/host \
	    --workdir /host/$(SUBDIR) \
	    -u $$DOCKER_UID:$$DOCKER_GID \
	    $(DOCKER_IMAGE) \
	    make test v=$v

docker-shell:: docker-build
	touch $(DOCKER_HISTORY)
	docker run --rm -it \
	    --volume $(ROOT):/host \
	    --volume $(DOCKER_HISTORY):/home/user/.bash_history \
	    --workdir /host/$(SUBDIR) \
	    --entrypoint /bin/bash \
	    $(DOCKER_IMAGE)

clean::
	$(RM) .project.clj
