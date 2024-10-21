include ../common/base.mk
include $(COMMON)/binding.mk

RUBY := $(shell command -v bundle)

RAKE_CMDS := \
    build \
    release \
    test \

export GEM_HOME=gem


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

ifdef RUBY
update:
	bundle install

$(RAKE_CMDS):: $(GEM_HOME)
	bundle exec rake $@

$(GEM_HOME):
	gem install minitest -v 5.20.0
	bundle install
endif

clean::
	$(RM) Gemfile.lock .rake_tasks*
	$(RM) -r pkg

realclean::
	$(RM) -r $(GEM_HOME)
