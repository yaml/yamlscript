# Used by util/version-bump to update versions in repo

v_api=0.1.34

v_perl=$v_api
v_python=$v_api
v_raku=$v_api
v_ruby=$v_api
v_rust=$v_api

# add ReadMe.md and www/src/index.md to this list

vp='0\.[01]\.[0-9]+'

#------------------------------------------------------------------------------
version=$v_api

pattern='(yamlscript.*)'"$vp"'(.*)'

bump .profile
bump common/project.clj
bump core/project.clj
bump libyamlscript/deps.edn
bump libyamlscript/project.clj
bump perl/lib/YAMLScript.pm
bump perl-alien/alienfile
bump perl-alien/lib/Alien/YAMLScript.pm
bump python/lib/yamlscript/__init__.py
bump rust/src/lib.rs
bump ys/deps.edn
bump ys/project.clj
bump ys/share/ys-0.bash
bump ys/src/yamlscript/cli.clj

pattern='(yamlscript\/core.*)'"$vp"'(.*)'

bump libyamlscript/project.clj
bump ys/project.clj

pattern='(YAMLSCRIPT.*)'"$vp"'(.*)'

bump raku/lib/YAMLScript.rakumod
bump ruby/lib/yamlscript.rb

pattern='(YAMLScript.*)'"$vp"'(.*)'

bump ReadMe.md
bump www/src/index.md

pattern='(version.*)'"$vp"'(.*)'

bump Meta

#------------------------------------------------------------------------------
version=$v_perl

pattern='(version.*)'"$vp"'(.*)'

bump perl/Meta
bump perl-alien/Meta

pattern='(VERSION.*)'"$vp"'(.*)'

bump perl/lib/YAMLScript.pm
bump perl-alien/lib/Alien/YAMLScript.pm

pattern='(Alien::YAMLScript:.*)'"$vp"'(.*)'

bump perl/Meta

#------------------------------------------------------------------------------
version=$v_python

pattern='(version.*)'"$vp"'(.*)'

bump python/setup.py

#------------------------------------------------------------------------------
version=$v_raku

pattern='(version.*)'"$vp"'(.*)'

bump raku/META6.json

#------------------------------------------------------------------------------
version=$v_ruby

pattern='(VERSION.*)'"$vp"'(.*)'

bump ruby/lib/yamlscript/version.rb

#------------------------------------------------------------------------------
version=$v_rust

pattern='(version.*)'"$vp"'(.*)'

bump rust/Cargo.toml
