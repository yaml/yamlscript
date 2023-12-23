#!/usr/bin/env raku

use Test;
use YAMLScript;

my %wanted = 
    foo => [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ],
;

my $program = q:to/.../;
!yamlscript/v0/data
foo:: 1..10
...

my YAMLScript $ys.=new;
is-deeply %wanted, $ys.load($program);
