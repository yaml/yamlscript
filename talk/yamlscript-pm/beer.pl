use v5.38;

use XXX;
use IO::All;

use YAMLScript;

my $yaml = io('beer.yaml')->all;

my $ys = YAMLScript->new();

XXX my $data = $ys->load($yaml);
