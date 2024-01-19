use YAMLScript;
use JSON;
my $ys = YAMLScript->new;
my $input = do { local $/; open my $fh, '<', 'file.ys'; <$fh> };
my $data = $ys->load($input);
print encode_json $data;
