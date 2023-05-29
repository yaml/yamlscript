use strict; use warnings;
package YAMLScript::CLI;

# XXX Local dev lib:
use lib "$ENV{HOME}/src/lingy/perl/lib";

use base 'Lingy::CLI';

sub main {
    require YAMLScript::Main;
    return YAMLScript::Main->new;
}

1;
