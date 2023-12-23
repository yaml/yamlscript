#!/usr/bin/env perl

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

is YAMLScript::FFI::load(<<'YS'
!yamlscript/v0/data
foo:: 1..10
YS
), {
    data => {
        foo => [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ],
    },
}, 'Returns data';

is YAMLScript::FFI::load(<<'YS'
yamlscript/v0/data
foo:: 1..10
YS
), {
    error => {
        type  => E,
        trace => E,
        cause => E,
    },
}, 'Returns error';

done_testing;
