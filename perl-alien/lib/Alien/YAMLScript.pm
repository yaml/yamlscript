use strict;
use warnings;

package Alien::YAMLScript;

our $VERSION = '0.1.97';

use parent 'Alien::Base';

our $libys_version = $VERSION;

die "Alien::YAMLScript $VERSION requires libys $libys_version" .
    "but you have " . __PACKAGE__->version
    unless Alien::YAMLScript->exact_version($libys_version);

1;
