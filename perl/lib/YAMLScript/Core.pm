use strict; use warnings;
package YAMLScript::Core;

use Lingy::Common;

sub ends_with_q {
    my ($str, $substr) = @_;
    $str = $$str;
    $substr = $$substr;
    BOOLEAN->new(
      length($str) >= length($substr) and
      substr($str, 0-length($substr)) eq $substr
    );
}

sub read_file_ys {
    my ($file) = @_;
    my $text = RT->slurp_file($file);
    RT->reader->read_ys($text, $file);
}

1;
