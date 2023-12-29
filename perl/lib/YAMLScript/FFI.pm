package YAMLScript::FFI;

use strict;
use warnings;

our $VERSION = '0.1.3';
our $libyamlscript_version = '0.1.34';

use Alien::YAMLScript;
use FFI::Platypus;
use FFI::CheckLib ();
use JSON ();

sub new {
    bless {}, shift;
}

unless ( Alien::YAMLScript->exact_version($libyamlscript_version) ) {
    my $have = Alien::YAMLScript->version;
    die "YAMLScript::FFI $VERSION requires Alien::YAMLScript libyamlscript version\n" .
        "$libyamlscript_version, but you have $have";
}

my $ffi = FFI::Platypus->new(
    api => 2,
    lib => [ Alien::YAMLScript->dynamic_libs ],
);

$ffi->function( graal_create_isolate => [qw( opaque opaque* opaque* )] => 'int' )
    ->( undef, \my $isolate, \my $thread)
    and die 'Could not setup evaluation';

$ffi->attach(
    [ load_ys_to_json => 'load' ] => [qw( sint64 string )] => 'string' => sub {
        my ( $xsub, $self, $ys ) = @_;
        my $resp = JSON::decode_json( $xsub->( $thread, $ys ) );

        return $resp->{data} if $resp->{data};

        if (my $error = $resp->{error}{cause}) {
            die "libyamlscript: $error";
        }

        die "YAMLScript::FFI unkown error";
    }
);

1;
