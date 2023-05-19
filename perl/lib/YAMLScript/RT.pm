use strict; use warnings;
package YAMLScript::RT;

use base 'Lingy::RT';

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
      (def! load-file (fn* [f]
        (cond
          (ends-with? f ".ys")
          (eval
            (read-file-ys f))

          (ends-with? f ".t")
          (eval
            (read-file-ys f))

          (ends-with? f ".ly")
          (eval
            (read-string
              (str
                "(do "
                (slurp f)
                "\nnil)")))

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

1;
