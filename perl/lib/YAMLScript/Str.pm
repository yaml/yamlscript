package YAMLScript::Str;
use Mo qw(xxx);

use YAMLScript::NS;

sub val {
    my ($self) = (@_);
    my $value = $$self;
    my $ns = NS;
    $value =~ s{
        \$(\w+)
    }{
        $ns->{$1} //
        ZZZ [
            # $ns,
            "Can't find '$1' in namepspace vars",
        ];
    }gex;
    $value;
}

1;
