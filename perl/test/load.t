#!/usr/bin/env perl

use Test2::V0 -target => 'YAMLScript';

my $program = <<'...';
!YS-v0:
foo:: 1 .. 10
...

is CLASS->new->load($program),
    { foo => [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ] },
    'Returns data';

# like dies { CLASS->new->load("mapping\nerror::") },
#     qr/libys: mapping values are not allowed/,
#     'Dies with libys error';

done_testing;
