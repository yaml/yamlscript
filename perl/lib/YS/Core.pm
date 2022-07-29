package YS::Core;
use Mo qw(xxx);

use YAMLScript::Util;
use YAMLScript::Native;

use boolean;
use Scalar::Util qw(looks_like_number);

sub define {
    my ($self, $ns) = @_;

    $YAMLScript::Native::ns = $ns;

    [
        def =>
        2 => sub {
            my ($name, $value) = @_;
            if (ref($value) eq 'YAMLScript::Func') {
                my $arity = @{$value->sign};
                $name = "${name}__$arity";
            }
            $ns->{$name} = $value;
        },
    ],

    [
        fn =>
        _ => \&YAMLScript::Native::fn,
        lazy => 1,
    ],

    [
        defn =>
        _ => sub {
            my ($name, $sign, @body) = @_;
            $sign = [$sign] unless ref($sign) eq 'ARRAY';
            my $arity = @$sign;
            $ns->{"${name}__$arity"} = YAMLScript::Func->new(
                ____ => $name,
                sign => $sign,
                body => [@body],
            );
        },
        min => 0,
        lazy => 1,
    ],

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
        eq =>
        2 => sub {
            return looks_like_number($_[0])
                ? ($_[0] == $_[1])
                : ($_[0] eq $_[1]);
        },
        op => '==',
    ],
    [
        ne =>
        2 => sub {
            return looks_like_number($_[0])
                ? ($_[0] != $_[1])
                : ($_[0] ne $_[1]);
        },
        op => '!=',
    ],
    [
        like =>
        2 => sub {
            my ($str, $re) = @_;
            $re =~ s{^/?(.*?)/?$}{$1};
            return $str =~ qr($re);
        },
        op => '=~',
    ],
    [
        unlike =>
        2 => sub {
            my ($str, $re) = @_;
            $re =~ s{^/?(.*?)/?$}{$1};
            return $str !~ qr($re);
        },
        op => '!~',
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
        if =>
        2 => sub {
            my ($cond, $then) = @_;
            if ($cond->call) {
                $then->call;
            }
        },
        3 => sub {
            my ($cond, $then, $else) = @_;
            if ($cond->call) {
                $then->call;
            }
            else {
                $else->call,
            }
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
