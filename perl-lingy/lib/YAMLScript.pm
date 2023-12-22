use strict; use warnings;
package YAMLScript;

our $VERSION = '0.1.20';

use FFI::Platypus;
use JSON;
use XXX;

my $yamlscript_version = '0.1.33';

my $so = $^O eq 'darwin' ? 'dylib' : 'so';
my $libys_name = 'libyamlscript.' . $so . '.' . $yamlscript_version;

# my $ld_library_path = $ENV{'LD_LIBRARY_PATH'};
# my @ld_library_paths = split(':', $ld_library_path) if $ld_library_path;
# push @ld_library_paths, '/usr/local/lib';

# my $libys_path = '';
# foreach my $path (@ld_library_paths) {
#     $path = $path . '/' . $libys_name;
#     if (-e $path) {
#         $libys_path = $path;
#         last;
#     }
# }

# die "Shared library file '$libys_name' not found."
#     unless $libys_path;

my $ffi = FFI::Platypus->new(
    api => 2,
    lib => $libys_name,
);

my $graal_create_isolate = $ffi->function(
    'graal_create_isolate',
    ['opaque', 'opaque', 'opaque'] => 'void'
);
my $load_ys_to_json = $ffi->function(
    'load_ys_to_json',
    ['opaque', 'string'] => 'string'
);

my $isolate = $ffi->new('opaque');
my $isolatethread = $ffi->new('opaque');
$graal_create_isolate->call(undef, $isolate, $isolatethread);

sub load {
    my ($ys_input) = @_;
    my $json_text = $load_ys_to_json->call($isolatethread, $ys_input);
    return decode_json($json_text);
}
