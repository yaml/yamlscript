include ../common/base.mk
include ../common/docker.mk

build:: node_modules main
ifdef DOCKERENV
	source ~/.bashrc && yarn build
else
	yarn build
endif
	cp src/install* build/
	cp src/blog-redirect-html build/posts/index.html
	cp src/advent-redirect-html build/posts/advent-2023/index.html

serve:: build
	yarn dev

site:: build gh-pages
	rm -fr gh-pages/*
	cp -r build/* gh-pages/

publish:: site
	git -C gh-pages add -A
	git -C gh-pages commit -m "$$(date)"
	git -C gh-pages push origin gh-pages

test::

clean::
	[[ -h node_modules ]] && $(RM) node_modules || true
	$(RM) -r build/ gh-pages/
	$(RM) main
	$(RM) yamlscript-1920x1080.png

distclean::
	$(RM) -r node_modules/

node_modules: package.json
ifdef DOCKERENV
	ln -s /$@ $@
else
	yarn install
endif

main:
	ln -s .. main

gh-pages:
	git branch --track $@ origin/$@ 2>/dev/null || true
	git worktree prune
	git worktree add $@

yamlscript-1920x1080.png: src/images/yamlscript.svg
	svgexport $< $@ 1920:1080 pad

docker-site:: docker-build gh-pages
	rm -fr node_modules
	docker run --rm -it \
	    --volume $(ROOT):/host \
	    --workdir /host/$(SUBDIR) \
	    -u $$DOCKER_UID:$$DOCKER_GID \
	    $(DOCKER_IMAGE) \
	    make site
	unlink node_modules

Dockerfile:: $(COMMON) Makefile
	cat \
	  $</docker-from-ubuntu.dockerfile \
	  $</docker-apt-base.dockerfile \
	  $</docker-useradd.dockerfile \
	  $</docker-apt-node.dockerfile \
	  $</docker-deps-node.dockerfile \
	  $</docker-apt-dev.dockerfile \
		> $@
