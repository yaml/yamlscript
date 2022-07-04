package YAMLScript::Compiler;
use Mo qw'build default xxx';

has file => ();
has yaml => ();
has data => {};
has json => ();

my $w = qr/(?:[a-z0-9])/;
my $p1 = qr/(?:[a-z]$w*)/;
my $id = qr/(?:$p1(?:-$w+)*)/;


use YAMLScript::Function;
use YAMLScript::Call;

use YAML::PP;
use YAML::PP::Schema;

sub BUILD {
    my ($self) = @_;

    my $yaml = $self->yaml || do {
        my $file = $self->file or die;
        open my $fh, '<', $file or die $!;
        my $yaml = do { local $/; <$fh> };
        close $fh;
        $self->yaml($yaml);
    };

    my $data = YAML::PP::Load($yaml);
    $self->data($data);
}

sub from_yaml {
    my ($self) = @_;
}

sub to_json {
    my ($self) = @_;
    require JSON::PP;
    JSON::PP::encode_json($self->data);
}

sub to_perl {}

sub compile {
    my ($self) = @_;

    my $data = $self->data;

    $data = { main => $data }
        if ref($data) eq 'ARRAY';

    die "Invalid YAMLScript, must be mapping or sequence"
        unless ref($data) eq 'HASH';

    my $code = YAMLScript::Function->new();

    for my $key (
        sort keys %$data
    ) {
        my $val = $data->{$key};

        if ($key eq '+use') {
            $val = [ $val ] unless ref($val) eq 'ARRAY';
            push @{$code->need}, @$val;
            next;
        }

        my $function = $self->parse_function($val, $key);
        $YAMLScript::Runtime::look->{$key} = $function;
        $code->func->{$key} = $function;
    }

    return $code;
}

sub parse_function {
    my ($self, $val, $name) = @_;
    ref($val) eq 'ARRAY' or die;
    @$val > 0 or die;

    my $args = [];
    LOOP:
    while (1) {
        last if $self->is_call($val->[0]);
        my $ref = ref($val->[0]);
        if (not $ref) {
            push @$args, shift @$val;
        }
        elsif ($ref eq 'ARRAY') {
            for my $v (@{$val->[0]}) {
                last LOOP if ref($v) or $v !~ /^$id$/;
            }
            @$args = @{shift(@$val)};
        }
        last;
    }

    my $body = $self->parse_values($val);

    {
        package func;
        sub {
            my $self = shift;
            my @args = map $self->val($_), @_;
            my $f = YAMLScript::Function->new(
                ____ => $name,
                args => $args,
                body => $body,
            )->call(@args);
        }
    }
}

sub parse_values {
    my ($self, $values) = @_;
    $values //= [];
    $values = [$values] unless ref($values) eq 'ARRAY';
    [ map $self->parse_value($_), @$values ];
}

sub parse_value {
    my ($self, $value) = @_;

    my ($name, $args);
    if ($name = $self->is_call($value, qr/\+/)) {
        $args = $self->parse_values($value->{"+$name"});
        $value = YAMLScript::Call->new(
            ____ => $name,
            args => $args,
        );
    }
    elsif ($name = $self->is_call($value, qr/\$/)) {
        $args = $self->parse_values($value->{"\$$name"});
        unshift @$args, $name;
        $value = YAMLScript::Call->new(
            ____ => 'set',
            args => $args,
        );
    }

    $value;
}

sub is_call {
    my ($self, $value, $char) = @_;
    $char //= qr/\+/;
    return 0 unless ref($value) eq 'HASH';
    my @keys = keys %$value;
    (
        @keys == 1 and
        $keys[0] =~ qr/^$char($id)$/
    )
    ? $1 : 0;
}
