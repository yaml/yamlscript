package DistTestFixer;

use strict;
use warnings;

use Config;
use File::Spec;
use File::Find;
use File::Path;

sub fix {
    my ($class, $bin_name, $perl_bin_name) = @_;
    $perl_bin_name //= $bin_name;

    my $postamble = '';
    if ( $^O eq 'MSWin32' ) {
        my $inc = File::Spec->catdir('inc', 'bin');
        if (not -d $inc) {
            File::Path::make_path($inc);

            my $perl_path = File::Spec->catfile($Config::Config{'installbin'}, 'perl');
            my $cpan_bin_path = File::Spec->catfile($inc, "$bin_name-cpan");
            my $cpan_cmd_path = File::Spec->catfile($inc, "$bin_name-cpan.cmd");
            my $perl_bin_path = File::Spec->catfile('bin', $perl_bin_name);

            file_write($cpan_cmd_path, qq{if exist "%~dpn0" perl %0 %*$/});

            my $text = file_read($perl_bin_path);
            file_write($cpan_bin_path, "#!$perl_path\n$text", 0777);

            for my $file (file_find('t', qr/\.t$/)) {
                my $text = file_read($file);
                if ($text =~ /\A.*$bin_name/) {
                    file_write($file, qq{#!$cpan_bin_path$/$text});
                }
            }
        }

        $postamble = <<'...';
export PATH := blib\script;$(PATH)

PERLPATH := $(FULLPERLRUN:"%"=%)

pure_all ::
	$(NOECHO) $(FULLPERLRUN) -p0i.bak -e "s(\$$PERL)($(PERLPATH))" blib\script\$bin_name
...
    }

    else {
        $postamble = <<'...';
PERLPATH := $(FULLPERLRUN:"%"=%)

FULLPERLRUN := PATH=blib/script:$(PATH) $(PERLPATH)

pure_all ::
	$(NOECHO) $(FULLPERLRUN) -p0i -e 's(\$$PERL)($(PERLPATH))' blib/script/$bin_name
...
    }

    $postamble =~ s/\$bin_name/$bin_name/;

    return $postamble;
}

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

1;
