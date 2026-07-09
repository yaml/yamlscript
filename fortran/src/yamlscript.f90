! Copyright 2023-2026 Ingy dot Net
! This code is licensed under MIT license (See License for details)

! Fortran binding/API for the libys shared library.
!
! This module is a Fortran port of the Python 'yamlscript' module, the
! reference implementation for YAMLScript FFI bindings to libys.
!
! The load() method takes a YAMLScript string as input and returns the
! raw JSON response string from libys: {"data": ...} on success or
! {"error": {"cause": ...}} on failure. Fortran has no standard JSON
! library, so parsing the response is left to the caller (for example
! with the json-fortran package).

! Low-level C binding interface for libys
module yamlscript_c
  use, intrinsic :: iso_c_binding
  implicit none

  interface
    ! Create a new GraalVM isolate
    function graal_create_isolate(params, isolate, isolate_thread) &
        bind(C, name='graal_create_isolate') result(rc)
      import :: c_ptr, c_int
      type(c_ptr), value :: params
      type(c_ptr), value :: isolate
      type(c_ptr) :: isolate_thread
      integer(c_int) :: rc
    end function graal_create_isolate

    ! Tear down a GraalVM isolate
    function graal_tear_down_isolate(isolate_thread) &
        bind(C, name='graal_tear_down_isolate') result(rc)
      import :: c_ptr, c_int
      type(c_ptr), value :: isolate_thread
      integer(c_int) :: rc
    end function graal_tear_down_isolate

    ! Compile and eval a YAMLScript string, returning a JSON response
    function load_ys_to_json_c(isolate_thread, ys) &
        bind(C, name='load_ys_to_json') result(json_ptr)
      import :: c_ptr, c_char
      type(c_ptr), value :: isolate_thread
      character(kind=c_char), intent(in) :: ys(*)
      type(c_ptr) :: json_ptr
    end function load_ys_to_json_c
  end interface

end module yamlscript_c


! High-level Fortran API for YAMLScript
module yamlscript
  use, intrinsic :: iso_c_binding
  use yamlscript_c
  implicit none
  private

  public :: yamlscript_t, yamlscript_version

  ! This value is automatically updated by 'make bump'.
  ! We currently only support binding to an exact version of libys.
  character(len=*), parameter :: yamlscript_version = '0.2.27'

  ! YAMLScript class/derived type
  type :: yamlscript_t
    private
    type(c_ptr) :: isolate_thread = c_null_ptr
  contains
    procedure :: init => yamlscript_init
    procedure :: destroy => yamlscript_destroy
    procedure :: load => yamlscript_load
  end type yamlscript_t

contains

  ! Initialize YAMLScript and create GraalVM isolate
  subroutine yamlscript_init(this)
    class(yamlscript_t), intent(inout) :: this
    integer(c_int) :: rc

    rc = graal_create_isolate(c_null_ptr, c_null_ptr, this%isolate_thread)
    if (rc /= 0) then
      error stop 'Failed to create GraalVM isolate'
    end if
  end subroutine yamlscript_init

  ! Destroy YAMLScript and tear down GraalVM isolate
  subroutine yamlscript_destroy(this)
    class(yamlscript_t), intent(inout) :: this
    integer(c_int) :: rc

    if (c_associated(this%isolate_thread)) then
      rc = graal_tear_down_isolate(this%isolate_thread)
      if (rc /= 0) then
        error stop 'Failed to tear down GraalVM isolate'
      end if
      this%isolate_thread = c_null_ptr
    end if
  end subroutine yamlscript_destroy

  ! Compile and eval a YAMLScript string and return the JSON response
  function yamlscript_load(this, ys) result(json)
    class(yamlscript_t), intent(in) :: this
    character(len=*), intent(in) :: ys
    character(len=:), allocatable :: json
    type(c_ptr) :: json_ptr
    character(len=:), allocatable :: ys_c

    if (.not. c_associated(this%isolate_thread)) then
      error stop 'YAMLScript not initialized'
    end if

    ! Convert Fortran string to C string
    ys_c = f_string_to_c(ys)

    ! Call C function
    json_ptr = load_ys_to_json_c(this%isolate_thread, ys_c)

    ! Convert C string to Fortran string
    json = c_ptr_to_string(json_ptr)
  end function yamlscript_load

  ! Helper: Convert Fortran string to C string (null-terminated)
  function f_string_to_c(f_string) result(c_string)
    character(len=*), intent(in) :: f_string
    character(len=:), allocatable :: c_string

    ! Append null terminator
    c_string = trim(f_string) // c_null_char
  end function f_string_to_c

  ! Helper: Convert C string pointer to Fortran allocatable string
  function c_ptr_to_string(c_str_ptr) result(f_string)
    type(c_ptr), intent(in) :: c_str_ptr
    character(len=:), allocatable :: f_string
    character(kind=c_char), dimension(:), pointer :: c_str_array
    integer :: i, str_len

    if (.not. c_associated(c_str_ptr)) then
      f_string = ""
      return
    end if

    ! Find string length (search for null terminator)
    str_len = 0
    call c_f_pointer(c_str_ptr, c_str_array, [1])
    do while (c_str_array(str_len + 1) /= c_null_char)
      str_len = str_len + 1
      call c_f_pointer(c_str_ptr, c_str_array, [str_len + 1])
    end do

    ! Allocate and copy
    allocate(character(len=str_len) :: f_string)
    call c_f_pointer(c_str_ptr, c_str_array, [str_len])
    do i = 1, str_len
      f_string(i:i) = c_str_array(i)
    end do
  end function c_ptr_to_string

end module yamlscript
