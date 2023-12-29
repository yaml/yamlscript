use strict; use warnings;
use YAMLScript;
use IO::All;
use Data::Dumper qw(Dumper);
$Data::Dumper::Indent = 1;
$Data::Dumper::Terse = 1;

my $ys = io('data.ys')->all;
my $data = YAMLScript->load($ys);

print Dumper($data);
