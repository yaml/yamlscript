package YS::XXX;
use Mo 'xxx';

use XXX ();

sub define {
    [
        www =>
        _ => sub { XXX::WWW(@_) }
    ],

    [
        xxx =>
        _ => sub { XXX::XXX(@_) }
    ],

    [
        yyy =>
        _ => sub { XXX::YYY(@_) }
    ],

    [
        zzz =>
        _ => sub { XXX::ZZZ(@_) }
    ],
}

1;
