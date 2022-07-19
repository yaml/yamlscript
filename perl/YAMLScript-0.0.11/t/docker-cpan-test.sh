#!/usr/bin/env bash

set -e -u -o pipefail

main() (
  if ! [[ -e /.dockerenv ]]; then
    echo "Test only works in Docker container"
    exit 1
  fi

  cd /tmp

  tar xf /host/YAMLScript-*.tar.gz

  cd YAMLScript-*

  /root/perl5/perlbrew/perls/perl-5.36.0/bin/perl /usr/bin/cpanm -n $(
    cpanm --showdeps . |
      tail -n+3 |
      grep -Ev '^(perl|ExtUtils)' 2>/dev/null |
      cut -d'~' -f1
  )

  /root/perl5/perlbrew/perls/perl-5.36.0/bin/perl Makefile.PL

  /usr/bin/make test
)

main "$@"
