use strict; use warnings;
package YAMLScript::Core;

use Lingy::Namespace;
use base 'Lingy::Namespace';
use Lingy::Common;

use constant NAME => 'ys.core';

our %ns = (
    fn('ends-with?'   => 2 => \&ends_with_q),
    fn('read-file-ys' => 1 => \&read_file_ys),
);

sub ends_with_q {
    my ($str, $substr) = @_;
    $str = $$str;
    $substr = $$substr;
    boolean(
      length($str) >= length($substr) and
      substr($str, 0-length($substr)) eq $substr
    );
}

sub read_file_ys {
    my ($file) = @_;
    my $text = YAMLScript::Main->slurp($file);
    my $reader = $YAMLScript::Main::reader;
    my $ast = $reader->read_ys($text, $file);
    return $ast;
}

1;
