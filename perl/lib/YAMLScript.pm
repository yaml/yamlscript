package YAMLScript;
use Mo qw'build xxx';

our $VERSION = '0.0.2';

use YAMLScript::Compiler;
use YAMLScript::Call;
use YAMLScript::Library;

has file => ();
has yaml => ();
has argv => [];
has code => {};

sub BUILD {
    my ($self) = @_;
    my $file = $self->file;
    my $yaml = $self->yaml;
    my $compiler = YAMLScript::Compiler->new(
        $file ? (file => $file) : (),
        $yaml ? (yaml => $yaml) : (),
    );

    %YAMLScript::Call::calls = (
        %YAMLScript::Call::calls,
        %YAMLScript::Library::calls,
    );

    my $func = $compiler->compile_global;

    $self->code($func);
}

sub run {
    my ($self) = @_;
    my $argv = $self->argv;
    my $code = $self->code;
    my $main = $code->var('main') or
        XXX [
            $self->{code},
            "No 'main' function to run",
        ];
    $main->($code, @$argv);
}

