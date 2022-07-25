package YS::TestYAMLScript;
use Mo qw(xxx);

use Test::More ();

my $count = 0;

sub define {
    [
        ok =>
        2 => sub {
            my ($got, $label) = @_;
            Test::More::ok(
                $got,
                $label,
            );
            $count++;
        },
    ],

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

    [
        fail =>
        1 => sub {
            my ($label) = @_;
            Test::More::fail(
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
