# Copyright 2023-2024 Ingy dot Net
# This code is licensed under MIT license (See License for details)

unit class YAMLScript;

use NativeCall;

constant YAMLSCRIPT_VERSION = v0.1.49;

sub load-json($json) { ::("Rakudo::Internals::JSON").from-json($json); }

sub load_ys_to_json(uint64, Str --> Str)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

sub graal_create_isolate(uint64 is rw, uint64 is rw, uint64 is rw --> uint64)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

sub graal_tear_down_isolate(uint64 --> uint64)
  is native('yamlscript', YAMLSCRIPT_VERSION) {*};

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
  die "Unexpected response from 'libyamlscript'"
    unless %data-json<data>:exists;
  %data-json<data>;
}
