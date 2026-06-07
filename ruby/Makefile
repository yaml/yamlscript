include ../common/base.mk
include $(COMMON)/binding.mk

include $(MAKES)/ruby.mk
include $(MAKES)/shell.mk

RAKE-CMDS := \
    build \
    release \
    test \

GEM-HOME := gem
export GEM_HOME := $(GEM-HOME)


#------------------------------------------------------------------------------

build:: build-doc

build-doc:: ReadMe.md

update:
	bundle install

$(RAKE-CMDS):: $(GEM-HOME)
	bundle exec rake $@

$(GEM-HOME): $(RUBY)
	gem install minitest -v 5.20.0
	bundle install

clean::
	$(RM) Gemfile.lock .rake_tasks*
	$(RM) -r pkg

realclean::
	$(RM) -r $(GEM-HOME)
