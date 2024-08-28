include ../common/base.mk
include $(ROOT)/common/docker.mk

DOC_FILES := $(shell cd $(ROOT)/doc && echo *.md)
DOC_FILES := $(DOC_FILES:%=src/doc/%) src/doc/doc.json

build:: node_modules main doc
ifdef DOCKERENV
	source ~/.bashrc && yarn build
else
	yarn build
endif
	cp src/dogs.yaml build/
	cp src/install* src/try* src/run* build/
	cp src/blog-redirect-html build/posts/index.html
	cp src/advent-redirect-html build/posts/advent-2023/index.html
	mkdir -p build/try
	cp $(ROOT)/sample/rosetta-code/99-bottles-of-beer.ys build/try/99-bottles.ys

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
	$(RM) -r build/ gh-pages/ src/doc
	$(RM) main
	$(RM) yamlscript-1920x1080.png

distclean::
	$(RM) yarn.lock
	$(RM) -r node_modules/

node_modules: package.json
ifdef DOCKERENV
	ln -s /$@ $@
else
	yarn install
endif

main:
	ln -s .. main

doc: gen-doc $(DOC_FILES)

gen-doc:
	$(MAKE) -C ../doc force build

src/doc/%.md: ../doc/%.md src/doc
	ln -fs ../../$< $@

src/doc/%: % src/doc
	ln -fs ../../$< $@

src/doc:
	mkdir -p $@

gh-pages:
	git branch --track $@ origin/$@ 2>/dev/null || true
	git worktree add -f $@

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
