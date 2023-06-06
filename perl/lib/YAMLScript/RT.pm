use strict; use warnings;
package YAMLScript::RT;

use Lingy::RT;
use base 'Lingy::RT';

use Lingy::Common;
use YAMLScript;
use YAMLScript::Core;
use YAMLScript::Reader;

use constant LANG => 'YAMLScript';
use constant reader_class => 'YAMLScript::Reader';

sub class_names {
    [
        @{Lingy::RT::class_names()},
        'Lingy::RT',
    ];
}

my $reader;
sub reader { $reader }

sub init {
    my $self = shift;
    $self->SUPER::init(@_);
    $reader = $self->require_new($self->reader_class);
    $self->rep(q< (use 'YAMLScript.Core) >);
    return $self;
}

sub core_namespace {
    my $self = shift;

    my $ns = $self->SUPER::core_namespace(@_);

    $YAMLScript::VERSION =~ /^(\d+)\.(\d+)\.(\d+)$/;
    $self->rep("
      (def *yamlscript-version*
        {
          :major       $1
          :minor       $2
          :incremental $3
          :qualifier   nil
        })
    ");

    return $ns;
}

sub is_lingy_class {
    my ($self, $class) = @_;
    $class->isa(CLASS) or $class =~ /^(?:Lingy|YAMLScript)::\w/;
}


# TODO Find cleaner way to override 'require' to support .ys files.
use Lingy::RT;
package Lingy::RT;
use Lingy::Common;

no warnings 'redefine';

sub require {
    outer:
    for my $spec (@_) {
        err "'require' only works with symbols"
            unless ref($spec) eq SYMBOL;

        return nil if $Lingy::Main::ns{$$spec};

        my $name = $$spec;

        my $path = $name;
        $path =~ s/^lingy\.lang\./Lingy./;
        $path =~ s/^lingy\./Lingy./;
        my $module = $path;
        $path =~ s/\./\//g;

        for my $inc (@INC) {
            $inc =~ s{^([^/.])}{./$1};
            my $inc_path = "$inc/$path";
            if (-f "$inc_path.ly" or
                -f "$inc_path.ys"
            ) {
                if (-f "$inc_path.ly") {
                    my $ns = RT->current_ns;
                    RT->rep(qq< (load-file "$inc_path.ly") >);
                    $ns->current;
                }
                if (-f "$inc_path.ys") {
                    my $ns = RT->current_ns;
                    RT->rep(qq< (load-file "$inc_path.ys") >);
                    $ns->current;
                }
                next outer;
            }
        }
        err "Can't find library for (require '$name)";
    }
    return nil;
}

1;
