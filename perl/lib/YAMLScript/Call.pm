package YAMLScript::Call;
use Mo qw'build default xxx';

use YAMLScript::Runtime;

has ____ => ();
has args => [];

sub call {
    my ($self, $from) = @_;
    my $name = $self->____;
    my $args = $self->args;
    my $func =
        $YAMLScript::Runtime::look->{$name} ||
        die "Can't find call name '$name'";
    $func->($from, @$args);
}
