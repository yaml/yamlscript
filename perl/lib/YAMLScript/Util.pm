package YAMLScript::Util;
use Mo qw(xxx);

use Exporter;
push @YAMLScript::Util::ISA, 'Exporter';
our @EXPORT = qw(
  add
  val
);

use YAMLScript::Call;
use Sub::Util 'set_subname';

sub add {
    my ($name, %params) = @_;

    my $m = delete $params{macro};
    my @macro;
    if (defined $m) {
        @macro = (macro => $m);
    }
    my $op = delete $params{op};
    my @defn;
    for my $arity (keys %params) {
        my $sub = $params{$arity};
        my $full = "${name}__$arity";
        my $code = (ref($sub) eq 'CODE')
            ? set_subname($full => $sub)
            : $sub;
        my $call = sub {
            YAMLScript::Call->new(
                ____ => $full,
                code => $code,
                args => $_[0],
                @macro,
            ),
        };
        push @defn, ($full => $call);
        if ($op) {
            push @defn, ("($op)__$arity" => $call);
        }
    }
    return @defn;
}

sub val {
    my ($self, $value) = @_;
    my $ref = ref($value);
    return $value
        if $ref eq 'YAMLScript::Function';
    if ($ref eq '') {
    }
    elsif ($ref eq 'YAMLScript::Expr') {
        $value = $value->call($self);
    }
    elsif ($ref eq 'ARRAY') {
        $value = [
            map $self->val($_), @$value
        ];
    }
    elsif ($ref eq 'YAMLScript::Str') {
        $value = $value->val;
    }
    else {
        XXX [$value, "ERROR: Unsupported value"];
    }
    return $value;
}
