#!/usr/bin/env perl

use Test2::V0 -target => 'Alien::YAMLScript';
use Test::Alien;
use Cpanel::JSON::XS;

alien_ok $CLASS;

ffi_ok { symbols => [ 'graal_create_isolate', 'load_ys_to_json' ] },
    with_subtest {
        my ($ffi) = @_;


        my $graal = $ffi->function( graal_create_isolate => [qw( opaque opaque* opaque* )] => 'int' )
            ->( undef, \my $isolate, \my $thread);

        is $graal, 0, 'Can create GraalVM isolate';

        my $load = $ffi->function(
            load_ys_to_json => [qw( sint64 string )] => 'string' => sub {
                my ( $xsub, $ys ) = @_;
                decode_json $xsub->( $thread, $ys );
            },
        );

        is $load->(<<'...'),
!YS-v0:
foo:: 1 .. 10
...
            { data => { foo => [ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 ] } },
            'Can load YS to JSON';
    };

done_testing;
