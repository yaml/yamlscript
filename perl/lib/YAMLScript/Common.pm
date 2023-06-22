use strict; use warnings;
package YAMLScript::Common;

use Exporter 'import';
use Lingy::Common;

BEGIN {
    package Lingy::Common;
    no warnings 'redefine';
    sub RT() { 'YAMLScript::RT' }
}

our @EXPORT = @Lingy::Common::EXPORT;

1;
