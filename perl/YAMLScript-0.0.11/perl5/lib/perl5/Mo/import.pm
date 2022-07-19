package Mo::import;
$Mo::import::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
my$i=\&import;*{$M.import}=sub{(@_==2 and not$_[1])?pop@_:@_==1?push@_,grep!/import/,@f:();goto&$i};
