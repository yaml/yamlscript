#!/usr/bin/env bash

# Written for https://github.com/yaml/yamlscript/issues/12
#
# Re: https://yamlscript.org/posts/advent-2023/dec-21/
#
# Run the example Python program that loads a YAML file with embedded YS
# commands.
#
# This script only assumes you have bash, python3 and curl
# It installs a local libys.so library and yamlscript.py module.

[[ $RERUN ]] || { RERUN=1 bash "$0" 2>&1 | tee "$0.txt"; exit; }

set -euo pipefail

set -x

dir=${0%.sh}

rm -fr "$dir"

mkdir -p "$dir"

cd "$dir"

python3 -mvenv venv

source venv/bin/activate &>/dev/null

pip3 -q install yamlscript

(
  which python3
  pip3 show yamlscript | grep Version:
)

prefix=$PWD/install

curl https://yamlscript.org/install | PREFIX=$prefix bash

(
  tree install
)

export LD_LIBRARY_PATH=$prefix/lib
# for macos:
export DYLD_LIBRARY_PATH=$LD_LIBRARY_PATH

cat <<... > db.yaml
cars:
- make: Ford
  model: Mustang
  year: 1967
  color: red
- make: Dodge
  model: Charger
  year: 1969
  color: orange
- make: Chevrolet
  model: Camaro
  year: 1969
  color: blue
...

cat <<... > racers.yaml
!YS-v0:

db =: load("db.yaml")

- name: Ingy dot Net
  car: ! db.cars.0
- name: Santa Claus
  car: ! db.cars.1
- name: Sir Lancelot
  car: ! db.cars.2
...

cat <<... > race-report.py
#!/usr/bin/env python

import yaml, yamlscript

data = yamlscript.YAMLScript().load(open('racers.yaml'))

print(yaml.dump(data))
...

python3 race-report.py
