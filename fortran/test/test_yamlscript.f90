! Copyright 2023-2026 Ingy dot Net
! This code is licensed under MIT license (See License for details)

! Test the yamlscript Fortran binding.
! load() returns the raw JSON response string from libys, so the
! assertions here check for expected substrings.

program test_yamlscript
  use yamlscript
  implicit none

  type(yamlscript_t) :: ys
  character(len=:), allocatable :: result
  integer :: fails

  fails = 0

  call ys%init()

  ! Load YS code:
  result = ys%load('!ys-0:' // new_line('a') // 'test:: inc(41)')
  call check(index(result, '"test":42') > 0, 'load ys code')

  ! Load plain YAML:
  result = ys%load('foo: bar')
  call check( &
    index(result, '"data"') > 0 .and. index(result, 'bar') > 0, &
    'load plain yaml')

  ! Load invalid input returns an error response:
  result = ys%load(':')
  call check(index(result, '"error"') > 0, 'load error response')

  ! Load multiple times on one instance:
  result = ys%load('!ys-0:' // new_line('a') // 'test:: inc(41)')
  call check(index(result, '"test":42') > 0, 'load multiple times')

  call ys%destroy()

  if (fails > 0) then
    print *, fails, 'test(s) failed'
    stop 1
  end if

contains

  subroutine check(cond, label)
    logical, intent(in) :: cond
    character(len=*), intent(in) :: label

    if (cond) then
      print *, 'ok - ', label
    else
      print *, 'not ok - ', label
      fails = fails + 1
    end if
  end subroutine check

end program test_yamlscript
