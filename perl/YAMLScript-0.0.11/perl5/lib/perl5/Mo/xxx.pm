package Mo::xxx;
$Mo::xxx::VERSION = '0.14';my$M="Mo::";
$VERSION='0.13';
use constant XXX_skip=>1;*{$M.'xxx::e'}=sub{my($P,$e)=@_;$e->{WWW}=sub{require XXX;XXX::WWW(@_)};$e->{XXX}=sub{require XXX;XXX::XXX(@_)};$e->{YYY}=sub{require XXX;XXX::YYY(@_)};$e->{ZZZ}=sub{require XXX;XXX::ZZZ(@_)}};
