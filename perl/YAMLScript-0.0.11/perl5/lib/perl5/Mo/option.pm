package Mo::option;
$Mo::option::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
*{$M.'option::e'}=sub{my($P,$e,$o)=@_;$o->{option}=sub{my($m,$n,%a)=@_;$a{option}or return$m;my$n2=$n;*{$P."read_$n2"}=sub{$_[0]->{$n2}};sub{$#_?$m->(@_):$m->(@_,1);$_[0]}}};
