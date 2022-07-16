package YS::Core;
use Mo qw(xxx);
use YAMLScript::Util;

use boolean;

sub define {
    my ($self, $ns) = @_;

    [
        add =>
        2 => sub { $_[0] + $_[1] },
        op => '+',
    ],
    [
        sub =>
        2 => sub { $_[0] - $_[1] },
        op => '-',
    ],
    [
        mul =>
        2 => sub { $_[0] * $_[1] },
        op => '*',
    ],
    [
        div =>
        2 => sub { $_[0] / $_[1] },
        op => '/',
    ],

    [
        comment =>
        _ => sub { return },
        lazy => 1,
        alias => [ qw(ignore) ],
    ],

    [
        conj =>
        2 => sub {
            my ($list, $val) = @_;
            push @$list, $val;
            return $list;
        },
    ],

    [
        def =>
        2 => sub {
            my ($var, $val) = @_;
            $ns->{$var} = $val;
            return $var;
        },
    ],

    [
        do =>
        _ => sub {
            $_->call for @_;
        },
        lazy => 1,
    ],

    [
        map =>
        2 => sub {
            my ($fn, $list) = @_;
            $list = $self->val($list);
            [
                map {
                    $ns->{_} = $_;
                    $fn->call($_);
                } @$list
            ];
        },
        lazy => 1,
    ],

    [
        'number?' =>
        1 => sub {
            $_[0] =~ /^(?:
                (
                    0
                |
                    -? [1-9] [0-9]*
                |
                    -?
                    [1-9]
                    ( \. [0-9]* [1-9] )?
                    ( e [-+] [1-9] [0-9]* )?
                )
            )$ /x ? true : false;
        },
    ],

    [
        for =>
        _ => sub {
            my ($list, @stmt) = @_;
            $list = $self->val($list);
            for (@$list) {
                $ns->{_} = $_;
                for my $stmt (@stmt) {
                    $stmt->call;
                }
            }
            delete $ns->{_};
            return;
        },
        lazy => 1,
    ],

    [
        len =>
        1 => sub {
            my ($string) = @_;
            return length $string;
        },
    ],

    [
        range =>
        2 => sub {
            my ($min, $max) = @_;
            return [ $min .. $max ];
        },
        op => '..',
    ],

    [
        say =>
        1 => sub {
            my ($string) = @_;
            local $ns->{foo} = 1;
            $string = $self->val($string);
            print $string . "\n";
        },
    ],
}

1;
