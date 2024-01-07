use strict;
use warnings;

package YAMLScript::Alien;

use Alien::YAMLScript;

# Alien::YAMLScript finds the proper libyamlscript version, but we need to be
# using the proper version of Alien::YAMLScript:
die "\$YAMLScript::VERSION ($YAMLScript::VERSION) and " .
    "\$Alien::YAMLScript::VERSION($Alien::YAMLScript::VERSION) " .
    "must be the same version"
    unless $YAMLScript::VERSION eq $Alien::YAMLScript::VERSION;
