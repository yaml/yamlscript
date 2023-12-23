#!/usr/bin/env perl

use Test2::V0 -target => 'YAMLScript::FFI';

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
