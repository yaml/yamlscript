package YAMLScript::NS;
use Mo qw(default xxx);

use Exporter;
push @YAMLScript::NS::ISA, 'Exporter';
our @EXPORT = qw(
    NS
    NS_push
    NS_pop
);

my @stack;

# UPPERCASE to avoid clashes with YS vars:
has NAME => ();
has NEED => [];

use YAMLScript::Call;

use List::MoreUtils qw(uniq);
use Sub::Util qw(set_subname);

sub NS { $stack[-1] }
sub NS_push { push @stack, @_ }
sub NS_pop  { pop  @stack }

sub NS_init {
    my ($self) = @_;
    my $i = 0;
    my @need = uniq(@{$self->NEED});
    for my $lib (@need) {
        if (not ref $lib) {
            (my $module = $lib) =~ s|-|::|g;
            (my $file = "$lib.pm") =~ s|-|/|g;
            require $file or die $!;
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
