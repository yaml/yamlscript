#!/usr/bin/env bash

main() {
  number=${1:-99}
  numbers=($(eval "echo {$number..1}"))
  for number in ${numbers[@]}; do
    echo "$(paragraph $number)"
    echo
  done
}

paragraph() {
  num=$1
  cat <<...
$(bottles $num) of Bash on the wall,
$(bottles $num) of Bash.
Take one down, cat it around,
$(bottles $((num - 1))) of Bash on the wall.
...
}

bottles() {
  case $1 in
    0) echo "No more bottles" ;;
    1) echo "1 bottle" ;;
    *) echo "$1 bottles" ;;
  esac
}

main "$@"
