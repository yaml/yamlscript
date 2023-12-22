unit module YAMLScript;

use NativeCall;

constant YAMLSCRIPT_VERSION = v0.1.34;

sub load_ys_to_json(uint32, Str --> Str)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

sub graal_create_isolate(uint32 is rw, uint32 is rw, uint32 is rw --> uint32)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

sub load-ys-to-json(Str:D $ys --> Str) is export {
  my uint32 ($a, $t, $c);

  my $rc = graal_create_isolate($a, $t, $c);
  die 'Failed to set up evaluation'
    if $rc != 0;

  load_ys_to_json($c, $ys);
}
