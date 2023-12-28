# Used by util/version-bump to update versions in repo

v_api=0.1.34
v_perl=0.1.1
v_python=0.1.4
v_raku=0.1.0
v_rust=0.1.2

api_files=(
  .profile
  Meta
  common/install
  common/project.clj
  core/project.clj
  libyamlscript/deps.edn
  libyamlscript/project.clj
  perl-alien/alienfile
  python/lib/yamlscript/loader.py
  raku/lib/YAMLScript.rakumod
  rust/src/lib.rs
  ys/deps.edn
  ys/project.clj
  ys/share/ys-0.bash
  ys/src/yamlscript/cli.clj
)

perl_files=(
  perl/Meta
  perl/lib/YAMLScript/FFI.pm
  perl-alien/Meta
  perl-alien/lib/Alien/YAMLScript.pm
)

python_files=(
  python/setup.py
)

raku_files=(
  raku/META6.json
)

rust_files=(
  rust/Cargo.toml
  rust/Cargo.lock
)

#------------------------------------------------------------------------------
yamlscript_files=(
  .profile
  common/project.clj
  core/project.clj
  libyamlscript/deps.edn
  libyamlscript/project.clj
  perl-alien/alienfile
  python/lib/yamlscript/loader.py
  rust/src/lib.rs
  ys/deps.edn
  ys/project.clj
  ys/share/ys-0.bash
  ys/src/yamlscript/cli.clj
)

YAMLSCRIPT_files=(
  raku/lib/YAMLScript.rakumod
)

yamlscript_core_files=(
  libyamlscript/project.clj
  ys/project.clj
)

version_files=(
  Meta
  perl/Meta
  perl-alien/Meta
  python/setup.py
  raku/META6.json
  rust/Cargo.toml
)

VERSION_files=(
  common/install
  perl/lib/YAMLScript/FFI.pm
  perl-alien/lib/Alien/YAMLScript.pm
)
