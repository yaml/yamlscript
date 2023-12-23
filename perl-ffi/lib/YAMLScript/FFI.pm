package YAMLScript::FFI;

use strict;
use warnings;

use FFI::Platypus;
use FFI::CheckLib ();
use JSON ();

my $ffi = FFI::Platypus->new(
    api => 2,
    lib => FFI::CheckLib::find_lib_or_die(
        lib => 'yamlscript',
    ),
);

$ffi->function( graal_create_isolate => [qw( opaque opaque* opaque* )] => 'int' )
    ->( undef, \my $isolate, \my $thread)
    and die 'Could not setup evaluation';

$ffi->attach(
    [ load_ys_to_json => 'load' ] => [qw( sint64 string )] => 'string' => sub {
        my ( $xsub, $ys ) = @_;
        my $data = $xsub->( $thread, $ys );
        JSON::decode_json($data);
    },
);

1;
