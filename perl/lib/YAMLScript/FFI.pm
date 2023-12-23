package YAMLScript::FFI;

use strict;
use warnings;

our $VERSION = '0.1.0';

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
    [ load_ys_to_json => '_load' ] => [qw( sint64 string )] => 'string' => sub {
        my ( $xsub, $ys ) = @_;
        my $data = $xsub->( $thread, $ys );
        JSON::decode_json($data);
    },
);

sub load {
    my ($ys_code) = @_;
    my $resp = _load($ys_code);
    if (my $data = $resp->{data}) {
        return $data;
    }
    elsif (my $error = $resp->{error}{cause}) {
        die "libyamlscript: $error";
    }
    else {
        die "YAMLScript::FFI unkown error";
    }
}

1;
