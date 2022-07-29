package YAMLScript::Native;
use Mo;

use YAMLScript::Func;

our $ns;

sub fn {
    my ($sign, @body) = @_;
    my $x = $ns;
    $sign = [$sign] unless ref($sign) eq 'ARRAY';
    my $arity = @$sign;
    YAMLScript::Func->new(
        sign => $sign,
        body => [@body],
    );
}


1;
