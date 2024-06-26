#!/usr/bin/env raku

use YAMLScript;

my $yaml = slurp 'beer2.yaml';

my YAMLScript $ys.=new;

say $ys.load($yaml);
