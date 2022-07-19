package Mo::importer;
$Mo::importer::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
*{$M.'importer::e'}=sub{my($P,$e,$o,$f)=@_;(my$pkg=$P)=~s/::$//;&{$P.'importer'}($pkg,@$f)if defined&{$P.'importer'}};
