package YAMLScript;

use strict;
use warnings;

our $VERSION = '0.1.21';

use Alien::YAMLScript;
use FFI::CheckLib ();
use FFI::Platypus;
use JSON ();

# Alien::YAMLScript finds the proper libyamlscript version, but we need to be
# using the proper version of Alien::YAMLScript:
die "\$YAMLScript::VERSION ($YAMLScript::VERSION) and " .
    "\$Alien::YAMLScript::VERSION($Alien::YAMLScript::VERSION) " .
    "must be the same version"
    unless $YAMLScript::VERSION eq $Alien::YAMLScript::VERSION;

# Set up FFI functions:
my $ffi = FFI::Platypus->new(
    api => 2,
    lib => [ Alien::YAMLScript->dynamic_libs ],
);

my $graal_create_isolate = $ffi->function(
    graal_create_isolate =>
        ['opaque', 'opaque*', 'opaque*'] => 'int',
);

my $graal_tear_down_isolate = $ffi->function(
    graal_tear_down_isolate =>
        ['opaque'] => 'int',
);

# YAMLScript object constuctor. Creates and saves a graal isolate thread:
sub new {
    my ($class, $config) = (@_, {});
    my ($isolatethread);
    $graal_create_isolate->(undef, undef, \$isolatethread) == 0
        or die 'Failed to create graal isolate';
    bless {
        isolatethread => \$isolatethread,
    }, $class;
}

# YAMLScript->load method.
# Load a YAMLScript code string to produce a Perl object:
sub _load {
    my ($xsub, $self, $ys) = @_;
    $self->{error} = undef;

    my $resp = JSON::decode_json(
        $xsub->(${$self->{isolatethread}}, $ys)
    );

    return $resp->{data} if exists $resp->{data};

    if ($self->{error} = $resp->{error}) {
        die "libyamlscript: $self->{error}{cause}";
    }

    die "Unexpected response from 'libyamlscript'";
}

# Attach the YAMLScript->load method to the libyamlscript load_ys_to_json
# function:
$ffi->attach(
    [load_ys_to_json => 'load'] =>
        ['sint64', 'string'] => 'string' =>
        \&_load,
);

# Tear down the graal isolate when the YAMLScript object goes out of scope:
sub DESTROY {
    my ($self) = @_;
    $graal_tear_down_isolate->(${$self->{isolatethread}}) == 0
        or die "Failed to tear down graal isolate";
}

1;
