package YS::TestYAMLScript;
use Mo qw'xxx';
use YS;
extends 'YS';

use Test::More ();

my $count = 0;

sub BUILD {
    my ($self) = @_;

    $self->func(
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
    );

    $self->func(
        pass =>
        1 => sub {
            my ($label) = @_;
            Test::More::pass(
                $label,
            );
            $count++;
        },
    );

}

sub END {
    if ($count > 0) {
        Test::More::done_testing($count);
    }
}

1;
