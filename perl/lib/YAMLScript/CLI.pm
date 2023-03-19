use strict; use warnings;
package YAMLScript::CLI;

# XXX Local dev lib:
use lib "$ENV{HOME}/src/lingy/perl/lib";

use base 'Lingy::CLI';

sub rt {
    require YAMLScript::RT;
    return YAMLScript::RT->new;
}

1;
