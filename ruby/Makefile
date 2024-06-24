include ../common/base.mk
include $(COMMON)/binding.mk

RUBY := $(shell command -v bundle)

RAKE_CMDS := \
    build \
    clean \
    release \
    test \

#------------------------------------------------------------------------------

build:: ReadMe.md

ifdef RUBY
update:
	bundle install

$(RAKE_CMDS)::
	bundle exec rake $@

endif

clean::
	$(RM) Gemfile.lock .rake_tasks*
	$(RM) -r pkg
