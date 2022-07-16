#!/usr/bin/env perl

use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/../lib";

use YAMLScript;

# TODO Parse CLI opts/args with Getopt::Long;

my $file = shift;

if (defined $file) {
    my $script = YAMLScript->new(
        file => $file,
    );
    $script->run(@ARGV);
}
else {
    require YAMLScript::REPL;
    my $repl = YAMLScript::REPL->new();
    $repl->run;
}
