include ../common/base.mk
include $(COMMON)/binding.mk

RUBY := $(shell command -v bundle)

RAKE-CMDS := \
    build \
    release \
    test \

GEM-HOME := gem
export GEM_HOME := $(GEM-HOME)


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef RUBY
update:
	bundle install

$(RAKE-CMDS):: $(GEM-HOME)
	bundle exec rake $@

$(GEM-HOME):
	gem install minitest -v 5.20.0
	bundle install
endif

clean::
	$(RM) Gemfile.lock .rake_tasks*
	$(RM) -r pkg

realclean::
	$(RM) -r $(GEM-HOME)
