package DistTestFixer;

use strict;
use warnings;

use Config;
use File::Spec;
use File::Find;
use File::Path;

sub file_find {
    my ($dir, $pat) = @_;
    my @files;
    File::Find::find {
        wanted => sub {
            if (-f and $_ =~ $pat) {
                push @files, $_;
            }
        },
        no_chdir => 1,
    }, $dir;
    return @files;
}

sub file_read {
    my ($file) = @_;
    open my $fh, '<', $file or die $!;
    my $text = do { local $/; <$fh> };
    close $fh;
    return $text;
}

sub file_write {
    my ($file, $text, $mode) = @_;
    open my $out, '>', $file or die $!;
    print $out $text;
    close $out;
    chmod $mode, $file if $mode;
    return 1;
}

sub fix {
    my ($class) = @_;
    my $inc = File::Spec->catdir('inc', 'bin');
    return if -d $inc;
    File::Path::make_path($inc);

    my $perl = File::Spec->catfile($Config::Config{'installbin'}, 'perl');

    if ( $^O eq 'MSWin32' ) {
        my $bin = File::Spec->catfile($inc, "yamlscript-cpan");
        my $cmd = File::Spec->catfile($inc, "yamlscript-cpan.cmd");
        my $ys =  File::Spec->catfile('bin', 'ys-yamlscript.pl');

        file_write($cmd, qq{if exist "%~dpn0" perl %0 %*$/});

        my $text = file_read($ys);
        file_write($bin, "#!$perl\n$text", 0777);

        for my $file (file_find('t', qr/\.t$/)) {
            my $text = file_read($file);
            if ($text =~ /\A.*yamlscript/) {
                file_write($file, qq{#!$bin$/$text});
            }
        }

        return <<'...';
export PATH := blib\script;$(PATH)

MYPERL := $(FULLPERLRUN:"%"=%)

pure_all ::
	$(NOECHO) $(FULLPERLRUN) -p0i.bak -e "s(\$$PERL)($(MYPERL))" blib\script\yamlscript
...
    }

    if ( $^O =~ /bsd/ ) {
        return <<'...';
MYPERL := PATH=blib/script:$(PATH) $(FULLPERLRUN:"%"=%)

pure_all ::
	$(NOECHO) $(FULLPERLRUN) -p0i -e 's(\$$PERL)($(MYPERL))' blib/script/yamlscript
...
    }

    return <<'...';
MYPERL := PATH=blib/script:$(PATH) $(FULLPERLRUN:"%"=%)

pure_all ::
	$(NOECHO) $(FULLPERLRUN) -p0i -e 's(\$$PERL)($(MYPERL))' blib/script/yamlscript
...
}

1;
