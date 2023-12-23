#!/usr/bin/env perl

use XXX;

BEGIN {
    if (not $ENV{PERL_YAMLSCRIPT_DEVEL}) {
        require Test::More;
        Test::More->import;
        pass("XXX - Tests need libyamlscript.so");
        done_testing();
        exit 0;
    }
}

use Test2::V0 -target => 'YAMLScript::FFI';
use YAMLScript::FFI;

my $program = <<'...';
!yamlscript/v0/data
foo:: 1..10
...

my $want = {
    foo => [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ],
};
my $got = YAMLScript::FFI->new->load($program);

is $got, $want, 'Returns data';

# XXX Need Try::Tiny to catch error?

# is YAMLScript::FFI->new->load(<<'...'
# yamlscript/v0/data
# foo:: 1..10
# ...
# ), {
#     error => {
#         type  => E,
#         trace => E,
#         cause => E,
#     },
# }, 'Returns error';

done_testing;
