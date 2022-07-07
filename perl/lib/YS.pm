package YS;
use Mo qw'build default xxx';
extends 'YAMLScript::Base';

has call => {};

use YAMLScript::NS;
use YAMLScript::Call;

use Sub::Name;

sub vars {
    ns->vars;
}

sub func {
    my ($self, $name, %pairs) = @_;
    while (my ($arity, $code) = each %pairs) {
        my $oper;
        if (ref($name) eq 'ARRAY') {
            ($name, $oper) = @$name;
        }
        my $full = "${name}__$arity";
        my $sub = $self->{$full} = subname $full => sub {
            YAMLScript::Call->new(
                ____ => $full,
                code => $code,
                args => $_[0],
            );
        };
        if (defined $oper) {
            $self->{"($oper)__$arity"} = $sub;
        }
    }
}

sub macro {
    my ($self, $name, %pairs) = @_;
    while (my ($arity, $code) = each %pairs) {
        my $oper;
        if (ref($name) eq 'ARRAY') {
            ($name, $oper) = @$name;
        }
        my $full = "${name}__$arity";
        my $sub = $self->{$full} = subname $full => sub {
            YAMLScript::Call->new(
                ____ => $full,
                code => $code,
                args => $_[0],
                macro => 1,
            );
        };
        if (defined $oper) {
            $self->{"($oper)__$arity"} = $sub;
        }
    }
}
