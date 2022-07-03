#!/usr/bin/perl

use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/../lib";

use YAMLScript::Compiler;

my $file = shift;

my $compiler = YAMLScript::Compiler->new(
    file => $file,
);
