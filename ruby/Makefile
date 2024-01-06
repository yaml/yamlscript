include ../common/base.mk
include $(COMMON)/binding.mk

RAKE := \
    build \
    clean \
    test \

#------------------------------------------------------------------------------

ifneq (,$(shell command -v bundle))
update:
	bundle install

$(RAKE)::
	bundle exec rake $@

endif

clean::
	$(RM) Gemfile.lock .rake_tasks*
