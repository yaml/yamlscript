use strict; use warnings;
package YAMLScript::Test;

use Lingy::Test;

use base 'Exporter';

use Test::More;

use YAMLScript::RT;
use YAMLScript::Reader;
use Lingy::Common;

our $rt = YAMLScript::RT->init;

$ENV{YAMLSCRIPT_TEST} = 1;

our $yamlscript =
    -f './blib/script/yamlscript' ? './blib/script/yamlscript' :
    -f './bin/yamlscript' ? './bin/yamlscript' :
    die "Can't find 'yamlscript' bin script to test";

our $eg =
    -d 'eg' ? 'eg' :
    -d 'example' ? 'example' :
    die "Can't find eg/example directory";

our @EXPORT = (
    @Lingy::Test::EXPORT,
    '$yamlscript',
);

sub collapse {
    local $_ = shift;
    s/\s\s+/ /g;
    s/^ //;
    chomp;
    $_;
}

sub line {
    local $_ = shift;
    s/\n/\\n/g;
    $_;
}

no warnings 'redefine';
sub test {
    my ($input, $want, $label) = @_;
    $label //= "'${\ collapse($input)}' -> '${\line $want}'";

    my $got = eval {
        local $YAMLScript::Reader::read_ys = 1;
        join("\n", $rt->rep($input));
    };
    $got = $@ if $@;
    chomp $got;

    $got =~ s/^Error: //;

    if (ref($want) eq 'Regexp') {
        like $got, $want, $label;
    } else {
        is $got, $want, $label;
    }
}

1;
