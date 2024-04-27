#!/usr/bin/env perl

use Test2::V0 -target => 'YAMLScript';

my $program = <<'...';
!yamlscript/v0/data
foo:: 1 .. 10
...

is CLASS->new->load($program),
    { foo => [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ] },
    'Returns data';

# like dies { CLASS->new->load("mapping\nerror::") },
#     qr/libyamlscript: mapping values are not allowed/,
#     'Dies with libyamlscript error';

done_testing;
