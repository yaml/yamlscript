# Used by util/version-bump to update versions in repo

v_api=0.1.42

v_perl=$v_api
v_python=$v_api
v_raku=$v_api
v_ruby=$v_api
v_rust=$v_api

vp='0\.[01]\.[0-9]+'

#------------------------------------------------------------------------------
version=$v_api

pattern='(yamlscript.*)'"$vp"'(.*)'

bump .profile
bump clojure/project.clj
bump common/project.clj
bump core/project.clj
bump libyamlscript/deps.edn
bump libyamlscript/project.clj
bump perl/lib/YAMLScript.pm
bump perl-alien/alienfile
bump perl-alien/lib/Alien/YAMLScript.pm
bump python/lib/yamlscript/__init__.py
bump rust/src/lib.rs
bump www/src/index.md
bump ys/deps.edn
bump ys/project.clj
bump clojure/deps.edn
bump ys/share/ys-0.bash
bump ys/src/yamlscript/cli.clj

pattern='(yamlscript\/core.*)'"$vp"'(.*)'

bump libyamlscript/project.clj
bump ys/project.clj

pattern='(YAMLSCRIPT.*)'"$vp"'(.*)'

bump common/install.mk
bump raku/lib/YAMLScript.rakumod
bump ruby/lib/yamlscript.rb
bump www/src/install
bump java/Makefile
bump java/src/main/java/org/yamlscript/yamlscript/YAMLScript.java

pattern='(YAMLScript.*)'"$vp"'(.*)'

bump ReadMe.md
bump www/src/index.md
bump ys/test/cli-usage.t

pattern='(version.*)'"$vp"'(.*)'

bump Meta
bump core/src/yamlscript/runtime.clj
bump java/pom.xml
bump www/src/index.md

pattern='(VERSION.*)'"$vp"'(.*)'

bump www/src/index.md
bump ys/test/cli-usage.t

pattern='(resource-paths.*)'"$vp"'(.*)'

bump clojure/project.clj

pattern='(yamlscript\/yamlscript.*)'"$vp"'(.*)'

bump clojure/project.clj

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
