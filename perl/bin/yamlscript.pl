#!/usr/bin/env perl

use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/../lib";

use YAMLScript;

my $file = shift;

YAMLScript->new(
    file => $file,
    argv => [@ARGV],
)->run;
