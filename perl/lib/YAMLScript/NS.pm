package YAMLScript::NS;
use Mo qw(default xxx);

use Exporter;
push @YAMLScript::NS::ISA, 'Exporter';
our @EXPORT = qw(
    ns
    ns_push
    ns_pop
);

my @stack;

has NAME => ();
has NEED => [];

use YAMLScript::Call;
use Sub::Util 'set_subname';

sub ns { $stack[-1] }
sub ns_push { push @stack, @_ }
sub ns_pop  { pop  @stack }

sub init {
    my ($self) = @_;
    my $i = 0;
    for my $lib (@{$self->NEED}) {
        if (not ref $lib) {
            (my $module = $lib) =~ s|-|::|g;
            (my $file = "$lib.pm") =~ s|-|/|g;
            require $file or die $!;
            # $self->NEED->[$i] = $lib;
            for my $def ($module->define($self)) {
                my ($name, %params) = @$def;
                my $m = delete $params{lazy};
                my @lazy;
                @lazy = (lazy => $m) if defined $m;
                my $op = delete $params{op};
                my $alias = delete $params{alias} // [];

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
                            @lazy,
                        ),
                    };
                    $self->{$full} = $call;
                    if ($op) {
                        $self->{"($op)__$arity"} = $call;
                    }
                    for (@$alias) {
                        $self->{"${_}__$arity"} = $call;
                    }
                }
            }
        }
        $i++;
    }
    return $self;
}

sub call {
    my ($self, $name, @args) = @_;

    my $call = $self->{$name} or
        die "Can't find callable '$name' in ns";

    $call->call(@args);
}
