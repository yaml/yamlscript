#!/bin/bash

set -xe

thisdir=$(dirname $0)
nativedir=$(cd $thisdir/../../../../../../native ; pwd)
rymldir=$(cd $nativedir/.. ; pwd)

make -C $nativedir build RAPIDYAML_TIMED=1
make -C $rymldir test RAPIDYAML_TIMED=1

cd $thisdir
jd=${jd:-/usr/lib/jvm/java-23-openjdk/bin}
$jd/javac -d . ../*.java
$jd/javac -d . -cp . CmpEdnEvt.java
$jd/jar -cmf manifest.mf CmpEdnEvt.jar cmp org
$jd/java -jar -Djava.library.path=$nativedir CmpEdnEvt.jar
