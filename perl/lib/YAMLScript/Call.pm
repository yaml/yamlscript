package YAMLScript::Call;
use Mo qw'build xxx';

use YAMLScript::Library;

our %calls;

has ____ => ();
has args => [];

sub call {
    my ($self, $from) = @_;
    my $name = $self->____;
    my $args = $self->args;
    my $func =
        $calls{$name} ||
        die "Can't find call name '$name'";
    $func->($from, @$args);
}
