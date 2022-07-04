package YAMLScript::Runtime;
use Mo qw'default xxx';

our $look = {};

has code => {};
has argv => [];
has look => {};

sub run {
    my ($self) = @_;

    my $code = $self->code;
    my $argv = $self->argv;

    for my $need (@{$code->need}) {
        (my $module = "$need") =~ s|-|::|g;
        (my $file = "$need.pm") =~ s|-|/|g;
        require $file or die $!;
        $module->add($look);
    }

    my $main = $code->func->{main} or
        XXX [
            $self->{code},
            "No 'main' function to run",
        ];

    $main->($code, @$argv);
}
