package YS::Core;
use Mo qw'xxx';
use YS;
extends 'YS';

# Use named subs for better stack traces

sub BUILD {
    my ($self) = @_;

    $self->func(
        [ add => '+' ] =>
        2 => sub {
            my ($x, $y) = @_;
            $x + $y;
        },
    );

    $self->func(
        conj =>
        2 => sub {
            my ($list, $val) = @_;
            push @$list, $val;
            $list;
        },
    );

    $self->func(
        def =>
        2 => sub {
            my ($var, $val) = @_;
            $self->vars->{$var} = $val;
            return $var;
        },
    );

    $self->macro(
        for =>
        2 => sub {
            my ($list, $action) = @_;
            $list = $self->val($list);
            for my $elem (@$list) {
                $self->vars->{_} = $elem;
                $action->call($self);
            }
        },
    );

    $self->func(
        len =>
        1 => sub {
            my ($string) = @_;
            length $string;
        },
    );

    $self->func(
        [ range => '..' ] =>
        2 => sub {
            my ($min, $max) = @_;
            [ $min .. $max ];
        },
    );

    $self->func(
        say =>
        1 => sub {
            # XXX YAMLScript::Scope::scope();
            my ($string) = @_;
            print $string . "\n";
        },
    );

    $self->func(
        [ sub => '-' ] =>
        2 => sub {
            my ($x, $y) = @_;
            $x - $y;
        },
    );
}
