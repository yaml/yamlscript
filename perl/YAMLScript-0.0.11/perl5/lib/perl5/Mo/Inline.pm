##
# name:      Mo::Inline
# abstract:  Inline Mo and Features into your package
# author:    Ingy döt Net <ingy@ingy.net>
# license:   perl
# copyright: 2011
# see:
# - Mo

package Mo::Inline;
use Mo;

our $VERSION='0.40';

use IO::All;

my $matcher = qr/((?m:^#\s*use Mo(\s.*)?;.*\n))(?:#.*\n)*(?:.{400,}\n)?/;

sub run {
    my $self = shift;
    my @files;
    if (not @_ and -d 'lib') {
        print "Searching the 'lib' directory for a Mo to inline:\n";
        @_ = 'lib';
    }
    if (not @_ or @_ == 1 and $_[0] =~ /^(?:-\?|-h|--help)$/) {
        print usage();
        return 0;
    }
    for my $name (@_) {
        die "No file or directory called '$name'"
            unless -e $name;
        die "'$name' is not a Perl module"
            if -f $name and $name !~ /\.pm$/;
        if (-f $name) {
            push @files, $name;
        }
        elsif (-d $name) {
            push @_, grep /\.pm$/, map { "$_" } io($name)->All_Files;
        }
    }

    die "No .pm files specified"
        unless @files;

    for my $file (@files) {
        my $text = io($file)->all;
        if ($text !~ $matcher) {
            print "Ignoring $file - No Mo to Inline!\n";
            next;
        }
        $self->inline($file, 1);
    }
}

sub inline {
    my ($self, $file, $noisy) = @_;
    my $text = io($file)->all;
    $text =~ s/$matcher/"$1" . &inliner($2)/eg;
    io($file)->print($text);
    print "Mo Inlined $file\n"
        if $noisy;
}

sub inliner {
    my $mo = shift;
    require Mo;
    my @features = grep {$_ ne 'qw'} ($mo =~ /(\w+)/g);
    for (@features) {
        eval "require Mo::$_; 1" or die $@;
    }
    my $inline = '';
    $inline .= $_ for map {
        my $module = $_;
        $module .= '.pm';
        my @lines = io($INC{$module})->chomp->getlines;
        $lines[-1];
    } ('Mo', map { s!::!/!g; "Mo/$_" } @features);
    return <<"...";
#   The following line of code was produced from the previous line by
#   Mo::Inline version $VERSION
$inline\@f=qw[@features];use strict;use warnings;
...
}

sub usage {
    <<'...';
Usage: mo-linline <perl module files or directories>

...
}

1;

=head1 SYNOPSIS

In your Mo module:

    # This is effectively your own private Mo(ose) setup
    package MyModule::Mo;
    # use Mo qw'build builder default import';
    1;

From the command line:

    > mo-inline lib/MyModule/Mo.pm

or:

    > mo-inline lib/

or (if you are really lazy):

    > mo-inline

Then from another module:

    package MyModule::Foo;
    use MyModule::Mo;       # gets build, builder and default automatically

=head1 DESCRIPTION

Mo is so small that you can easily inline it, along with any feature modules.
Mo provides a script called C<mo-inline> that will do it for you.

All you need to do is comment out the line that uses Mo, and run C<mo-inline>
on the file. C<mo-inline> will find such comments and do the inlining for you.
It will also replace any old inlined Mo with the latest version.

What Mo could you possibly want?

=head1 AUTOMATIC FEATURES

By using the L<Mo::import> feature, all uses of your Mo class will turn on all
the features you specified. You can override it if you want, but that will be
the default.

=head1 REAL WORLD EXAMPLES

For real world examples of Mo inlined using C<mo-inline>, see L<YAML::Mo>,
L<Pegex::Mo> and L<TestML::Mo>.
