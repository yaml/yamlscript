package Mo::nonlazy;
$Mo::nonlazy::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
*{$M.'nonlazy::e'}=sub{${shift().':N'}=1};
