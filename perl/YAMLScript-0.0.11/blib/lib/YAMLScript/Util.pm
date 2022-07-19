package YAMLScript::Util;
use Mo qw(xxx);

use Exporter;
push @YAMLScript::Util::ISA, 'Exporter';
our @EXPORT = qw(
  val
);

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
