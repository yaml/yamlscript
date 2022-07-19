package Mo::exporter;
$Mo::exporter::VERSION = '0.40';my$M="Mo::";
$VERSION='0.40';
*{$M.'exporter::e'}=sub{my($P)=@_;if(@{$M.EXPORT}){*{$P.$_}=\&{$M.$_}for@{$M.EXPORT}}};
