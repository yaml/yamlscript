package Mo::xs;
$Mo::xs::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
require Class::XSAccessor;*{$M.'xs::e'}=sub{my($P,$e,$o,$f)=@_;$P=~s/::$//;$e->{has}=sub{my($n,%a)=@_;Class::XSAccessor->import(class=>$P,accessors=>{$n=>$n})}if!grep!/^xs$/,@$f};
