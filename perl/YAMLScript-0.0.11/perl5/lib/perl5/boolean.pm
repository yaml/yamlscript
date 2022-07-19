use strict; use warnings;
package boolean;
our $VERSION = '0.46';

my ($true, $false);

use overload
    '""' => sub { ${$_[0]} },
    '!' => sub { ${$_[0]} ? $false : $true },
    fallback => 1;

use base 'Exporter';
@boolean::EXPORT = qw(true false boolean);
@boolean::EXPORT_OK = qw(isTrue isFalse isBoolean);
%boolean::EXPORT_TAGS = (
    all    => [@boolean::EXPORT, @boolean::EXPORT_OK],
    test   => [qw(isTrue isFalse isBoolean)],
);

sub import {
    my @options = grep $_ ne '-truth', @_;
    $_[0]->truth if @options != @_;
    @_ = @options;
    goto &Exporter::import;
}

my ($true_val, $false_val, $bool_vals);

BEGIN {
    my $t = 1;
    my $f = 0;
    $true  = do {bless \$t, 'boolean'};
    $false = do {bless \$f, 'boolean'};

    $true_val  = overload::StrVal($true);
    $false_val = overload::StrVal($false);
    $bool_vals = {$true_val => 1, $false_val => 1};
}

# refaddrs change on thread spawn, so CLONE fixes them up
sub CLONE {
    $true_val  = overload::StrVal($true);
    $false_val = overload::StrVal($false);
    $bool_vals = {$true_val => 1, $false_val => 1};
}

sub true()  { $true }
sub false() { $false }
sub boolean($) {
    die "Not enough arguments for boolean::boolean" if scalar(@_) == 0;
    die "Too many arguments for boolean::boolean" if scalar(@_) > 1;
    return not(defined $_[0]) ? false :
    "$_[0]" ? $true : $false;
}
sub isTrue($)  {
    not(defined $_[0]) ? false :
    (overload::StrVal($_[0]) eq $true_val)  ? true : false;
}
sub isFalse($) {
    not(defined $_[0]) ? false :
    (overload::StrVal($_[0]) eq $false_val) ? true : false;
}
sub isBoolean($) {
    not(defined $_[0]) ? false :
    (exists $bool_vals->{overload::StrVal($_[0])}) ? true : false;
}

sub truth {
    die "-truth not supported on Perl 5.22 or later" if $] >= 5.021005;
    # enable modifying true and false
    &Internals::SvREADONLY( \ !!0, 0);
    &Internals::SvREADONLY( \ !!1, 0);
    # turn perl internal booleans into blessed booleans:
    ${ \ !!0 } = $false;
    ${ \ !!1 } = $true;
    # make true and false read-only again
    &Internals::SvREADONLY( \ !!0, 1);
    &Internals::SvREADONLY( \ !!1, 1);
}

sub TO_JSON { ${$_[0]} ? \1 : \0 }

1;
