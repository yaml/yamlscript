# Copyright 2023-2025 Ingy dot Net
# This code is licensed under MIT license (See License for details)

use strict;
use warnings;

package YAMLScript;

use FFI::CheckLib ();
use FFI::Platypus;
use Cpanel::JSON::XS ();

our $VERSION = '0.1.97';

our $libys_version = $VERSION;


#------------------------------------------------------------------------------
# libys FFI setup:
#------------------------------------------------------------------------------

# Find the proper libys version:
my $libys = find_libys();

# Set up FFI functions:
my $ffi = FFI::Platypus->new(
    api => 2,
    lib => $libys,
);

my $graal_create_isolate = $ffi->function(
    graal_create_isolate =>
        ['opaque', 'opaque*', 'opaque*'] => 'int',
);

my $graal_tear_down_isolate = $ffi->function(
    graal_tear_down_isolate =>
        ['opaque'] => 'int',
);


#------------------------------------------------------------------------------
# YAMLScript object constructor and destructor:
#------------------------------------------------------------------------------

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

# Tear down the graal isolate when the YAMLScript object goes out of scope:
sub DESTROY {
    my ($self) = @_;
    $graal_tear_down_isolate->(${$self->{isolatethread}}) == 0
        or die "Failed to tear down graal isolate";
}

#------------------------------------------------------------------------------
# YAMLScript API methods:
#------------------------------------------------------------------------------
sub load;
# "load" method wrapper for FFI.
# It calls the libys load_ys_to_json function.
$ffi->attach(
    [load_ys_to_json => 'load'] =>
    ['sint64', 'string'] => 'string' =>
    sub {
        my ($xsub, $self, $ys) = @_;
        $self->{error} = undef;

        my $resp = Cpanel::JSON::XS::decode_json(
            $xsub->(${$self->{isolatethread}}, $ys)
        );

        return $resp->{data} if exists $resp->{data};

        if ($self->{error} = $resp->{error}) {
            die "libys: $self->{error}{cause}";
        }

        die "Unexpected response from 'libys'";
    },
);

#------------------------------------------------------------------------------
# Helper functions:
#------------------------------------------------------------------------------
# Look for the local libys first, then look for the Alien version:
sub find_libys {
    my $vers = $libys_version;
    my $so = $^O eq 'darwin' ? 'dylib' : 'so';
    my $name = "libys.$so.$vers";
    my @paths;
    if (my $path = $ENV{LD_LIBRARY_PATH}) {
        @paths = split /:/, $path;
    }
    push @paths, qw(
        /usr/local/lib
        /usr/local/lib64
        /usr/lib
        /usr/lib64
    ), "$ENV{HOME}/.local/lib";
    for my $path (@paths) {
        if (-e "$path/$name") {
            return "$path/$name";
        }
    }

    require Alien::YAMLScript;

    for my $path (Alien::YAMLScript->dynamic_libs) {
        if ($path =~ /\Q$name\E$/ && -r $path) {
            return $path;
        }
    }

    die <<"..."
Shared library file $name not found
Try: curl https://yamlscript.org/install | VERSION=$vers LIB=1 bash
See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
...
}

1;
