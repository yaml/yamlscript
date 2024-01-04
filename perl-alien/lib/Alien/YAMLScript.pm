package Alien::YAMLScript;

our $VERSION = '0.1.21';

use strict;
use warnings;

use parent 'Alien::Base';

our $libyamlscript_version = '0.1.34';

die "Alien::YAMLScript $VERSION requires libyamlscript $libyamlscript_version" .
    "but you have " . __PACKAGE__->version
    unless Alien::YAMLScript->exact_version($libyamlscript_version);

1;
