package YS::TestYAMLScript;
use Mo qw(xxx);
use YAMLScript::Util;

use Test::More ();

my $count = 0;

sub define {
    [
        is =>
        3 => sub {
            my ($got, $want, $label) = @_;
            Test::More::is(
                $got,
                $want,
                $label,
            );
            $count++;
        },
    ],

    [
        pass =>
        1 => sub {
            my ($label) = @_;
            Test::More::pass(
                $label,
            );
            $count++;
        },
    ],
}

sub END {
    if ($count > 0) {
        Test::More::done_testing($count);
    }
}

1;
