package YAMLScript::NS;
use Mo qw'default xxx';
use Exporter;
push @YAMLScript::NS::ISA, 'Exporter';
our @EXPORT = qw'ns ns_push ns_pop';

our @stack;

has ____ => ();
has base => ();
has vars => {};
has need => [];

use YAMLScript::Call;

sub ns { $stack[-1] }
sub ns_push { push @stack, @_ }
sub ns_pop  { pop  @stack }

sub call {
    my ($self, $name, @args) = @_;

    my $call = $self->vars->{$name} or
        die "Can't find callable '$name' in ns";

    $call->call(@args);
}

sub resolve {
    my ($self, $name, $arity) = @_;
    my $full = "${name}__$arity";

    my $many = "${name}___";

    my $i = 0;
    for my $lib (@{$self->need}) {
        if (not ref $lib) {
            (my $module = $lib) =~ s|-|::|g;
            (my $file = "$lib.pm") =~ s|-|/|g;
            require $file or die $!;
            $lib = $module->new;
            $self->need->[$i] = $lib;
        }
        $i++;
        if (my $call = $self->vars->{$name}) {
            return sub {
                YAMLScript::Call->new(
                    ____ => $name,
                    code => $call,
                    args => $_[0],
                );
            };
        }
        if (my $call = $lib->{$full}) {
            return $call;
        }
        if (my $call = $lib->{$many}) {
            return $call;
        }
    }

    die "Can't resolve call '$name' (for arity '$arity')";
}
