package Mo::chain;
$Mo::chain::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
*{$M.'chain::e'}=sub{my($P,$e,$o)=@_;$o->{chain}=sub{my($m,$n,%a)=@_;$a{chain}or return$m;sub{$#_?($m->(@_),return$_[0]):$m->(@_)}}};
