package Mo::coerce;
$Mo::coerce::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
*{$M.'coerce::e'}=sub{my($P,$e,$o)=@_;$o->{coerce}=sub{my($m,$n,%a)=@_;$a{coerce}or return$m;sub{$#_?$m->($_[0],$a{coerce}->($_[1])):$m->(@_)}};my$C=$e->{new}||*{$M.Object::new}{CODE};$e->{new}=sub{my$s=$C->(@_);$s->$_($s->{$_})for keys%$s;$s}};
