use strict; use warnings;
package YAMLScript::Core;

use File::Spec;

use YAMLScript::Common;

sub dirname {
    my ($file_path) = @_;
    use File::Spec;
    my(undef, $dirname, undef) = File::Spec->splitpath($file_path);
    STRING->new($dirname);
}

sub ends_with_q {
    my ($str, $substr) = @_;
    BOOLEAN->new(
      length("$str") >= length("$substr") and
      substr("$str", 0-length("$substr")) eq "$substr"
    );
}

sub read_file_ys {
    my ($file) = @_;
    my $text = RT->slurp_file($file);
    RT->reader->read_ys($text, $file);
}

sub read_string_ys {
    my ($string) = @_;
    RT->reader->read_ys($string, undef);
}

1;
