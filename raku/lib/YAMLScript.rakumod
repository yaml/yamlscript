unit class YAMLScript;

use NativeCall;

constant YAMLSCRIPT_VERSION = v0.1.34;

sub from-j($t) { ::("Rakudo::Internals::JSON").from-json($t); }

sub load_ys_to_json(uint64, Str --> Str)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

sub graal_create_isolate(uint64 is rw, uint64 is rw, uint64 is rw --> uint64)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

has uint64 $!thread;

submethod BUILD {
  my uint64 ($a, $t, $c);
  my $rc = graal_create_isolate($a, $t, $c);
  die 'Failed to set up evaluation'
    if $rc != 0;
  $!thread = $c;
}

method load(Str $program) {
  my %res = from-j load_ys_to_json($!thread, $program);
  if %res<error>:exists {
    die %res<error><cause>;
  } else {
    %res<data>;
  }
}
