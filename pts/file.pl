use v5.38;
use YAMLScript;
use JSON;

my $ys = YAMLScript->new();
open my $fh, 'file.yaml';
my $yaml = do {local $/; <$fh>};
close $fh;
my $json = JSON->new->allow_nonref;
my $data = $ys->load($yaml);

say $json->pretty->encode($data);














