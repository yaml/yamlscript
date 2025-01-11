include ../common/base.mk
include $(COMMON)/python.mk

PYTHON_VENV := $(ROOT)/www/.venv
VENV := source $(PYTHON_VENV)/bin/activate &&
CONFIG := mkdocs.yml
META := $(ROOT)/Meta

# DOC_FILES := $(shell cd $(ROOT)/doc && echo *.md)
# DOC_FILES := $(DOC_FILES:%=src/doc/%) src/doc/doc.json

foo:
	genys aaa bbb

build:: venv $(CONFIG)
	$(VENV) mkdocs build

# 	cp src/dogs.yaml build/
# 	cp src/install* src/try* src/run* build/
# 	cp src/blog-redirect-html build/posts/index.html
# 	cp src/advent-redirect-html build/posts/advent-2023/index.html
# 	mkdir -p build/try
# 	cp $(ROOT)/sample/rosetta-code/99-bottles-of-beer.ys build/try/99-bottles.ys

serve:: venv
	$(VENV) mkdocs $@

site:: build gh-pages
	rm -fr gh-pages/*
	cp -r build/* gh-pages/

publish:: site

# 	git -C gh-pages add -A
# 	git -C gh-pages commit -m "$$(date)"
# 	git -C gh-pages push origin gh-pages

test::

clean::
	$(RM) -r site

# 	$(RM) yamlscript-1920x1080.png

realclean:: clean
	$(RM) -r $(PYTHON_VENV)

venv: $(PYTHON_VENV)

$(PYTHON_VENV):
	$(PYTHON) -mvenv $@
	$(VENV) pip install \
	  mkdocs-material \
	  mkdocs-awesome-pages-plugin \

%.yml: %.ys
	genys $< $@


# doc: gen-doc $(DOC_FILES)
# 
# gen-doc:
# 	$(MAKE) -C ../doc force build
# 
# src/doc/%.md: ../doc/%.md src/doc
# 	ln -fs ../../$< $@
# 
# src/doc/%: % src/doc
# 	ln -fs ../../$< $@
# 
# src/doc:
# 	mkdir -p $@
# 
# gh-pages:
# 	git branch --track $@ origin/$@ 2>/dev/null || true
# 	git worktree add -f $@
# 
# yamlscript-1920x1080.png: src/images/yamlscript.svg
# 	svgexport $< $@ 1920:1080 pad
