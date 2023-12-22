#!/usr/bin/env perl

use strict; use warnings;

use YAMLScript::Main;

$ENV{LINGY_USAGE} = $ENV{YAMLSCRIPT_USAGE};

YAMLScript::Main->new->run(@ARGV);
