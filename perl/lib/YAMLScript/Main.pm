use strict; use warnings;
package YAMLScript::Main;

use base 'Lingy::Main';

use Lingy::Common;
use YAMLScript::Core;
use YAMLScript::Reader;

use constant LANG => 'YAMLScript';
use constant reader_class => 'YAMLScript::Reader';

our ($rt, $reader);

sub new {
    my $class = shift;
    $rt = $class->SUPER::new(@_);
}

sub rt { $rt }

sub init {
    my $self = shift;
    $self->SUPER::init(@_);
    $reader = $self->require_new($self->reader_class);
    $self->rep(q<
      (def load-file (fn [f]
        (cond
          (ends-with? f ".ys")
          (eval (read-file-ys f))

          (ends-with? f ".ly")
          (-load-file-ly f)

          :else
          (throw (str "Can't load-file '" f "'\n"))
      )))>);
    return $self;
}

sub user_namespace {
    my ($self) = @_;

    Lingy::Namespace->new(
        name => 'user',
        refer => [
            $self->core,
            $self->util,
            YAMLScript::Core->new,
        ],
    );
}

sub repl {
    local $YAMLScript::Reader::read_ys = 1;
    my $self = shift;
    $self->SUPER::repl(@_);
}


# TODO Find cleaner way to override 'require' to support .ys files.
use Lingy::Lang::RT;
package Lingy::Lang::RT;
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
        $path =~ s/^lingy\.lang\./Lingy.Lang\./;
        $path =~ s/^lingy\./Lingy\./;
        my $module = $path;
        $path =~ s/\./\//g;

        for my $inc (@INC) {
            $inc =~ s{^([^/.])}{./$1};
            my $inc_path = "$inc/$path";
            if (-f "$inc_path.pm" or
                -f "$inc_path.ly" or
                -f "$inc_path.ys"
            ) {
                if (-f "$inc_path.pm") {
                    CORE::require("$inc_path.pm");
                    $module =~ s/\./::/g;
                    err "Can't require $name. " .
                        "$module is not a Lingy::Namespace."
                        unless $module->isa('Lingy::Namespace');
                    $module->new(
                        name => symbol($name),
                        refer => Lingy::Main->core,
                    );
                }
                if (-f "$inc_path.ly") {
                    my $ns = $Lingy::Main::ns{$Lingy::Main::ns};
                    Lingy::Main->rep(qq< (load-file "$inc_path.ly") >);
                    $ns->current;
                }
                if (-f "$inc_path.ys") {
                    my $ns = $Lingy::Main::ns{$Lingy::Main::ns};
                    Lingy::Main->rep(qq< (load-file "$inc_path.ys") >);
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
