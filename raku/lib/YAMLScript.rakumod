# Copyright 2023-2026 Ingy dot Net
# This code is licensed under MIT license (See License for details)

unit class YAMLScript;

use NativeCall;

constant YAMLSCRIPT_VERSION = v0.2.28;

sub resolve-lib {
  state $ = do {
    my $libname;
    my @lib-paths;
    if $*DISTRO.is-win {
      $libname = 'libys.dll';
      @lib-paths = (%*ENV<PATH>//'').split(';', :ignore-empty);
    }
    else {
      my $so = $*KERNEL.name eq 'darwin' ?? '.dylib' !! '.so';
      $libname = "libys{$so}.{YAMLSCRIPT_VERSION.Str}";
      @lib-paths = (%*ENV<LD_LIBRARY_PATH>//'').split(':', :ignore-empty);
      @lib-paths.push: '/usr/local/lib';
    }
    my $home = %*ENV<HOME> // %*ENV<USERPROFILE>;
    @lib-paths.push($home ~ '/.local/lib') if $home;
    my $path = @lib-paths
      .grep(* ne '').first({ $_.IO.add($libname).e ?? True !! Nil });
    unless $path {
      my $vers = YAMLSCRIPT_VERSION;
      $*ERR.say: qq:to/EOM/;
      Shared library file '{$libname}' not found
      Try: curl https://yamlscript.org/install | VERSION=$vers LIB=1 bash
      See: https://github.com/yaml/yamlscript/wiki/Installing-YAMLScript
      EOM
      exit 1;
    }
    $path.IO.add($libname).absolute;
  }
}

sub load-json($json) { ::("Rakudo::Internals::JSON").from-json($json); }

sub load_ys_to_json(uint64, Str --> Str)
  is native(&resolve-lib) {*};

sub graal_create_isolate(uint64 is rw, uint64 is rw, uint64 is rw --> uint64)
  is native(&resolve-lib) {*};

sub graal_tear_down_isolate(uint64 --> uint64)
  is native(&resolve-lib) {*};

has uint64 $!isolate-thread;

submethod BUILD {
  my uint64 ($n1, $n2);
  my $rc = graal_create_isolate($n1, $n2, $!isolate-thread);
  die "Failed to create isolate"
    if $rc != 0;
}

submethod DESTROY {
  my $rc = graal_tear_down_isolate($!isolate-thread);
  die "Failed to tear down isolate"
    unless $rc == 0;
}

method load(Str $program) {
  my %data-json = load-json
    load_ys_to_json($!isolate-thread, $program);
  die %data-json<error><cause>
    if %data-json<error>:exists;
  die "Unexpected response from 'libys'"
    unless %data-json<data>:exists;
  %data-json<data>;
}
