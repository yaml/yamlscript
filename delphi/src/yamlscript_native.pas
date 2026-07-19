{ Copyright 2023-2026 Ingy dot Net
  This code is licensed under MIT license (See License for details) }

unit yamlscript_native;

{$mode objfpc}{$H+}

interface

uses
  ctypes;

const
  { This value is automatically updated by 'make bump'.
    We currently only support binding to an exact version of libys. }
  {$IFDEF LINUX}
  LIBYS = 'libys.so.0.2.29';
  {$ENDIF}
  {$IFDEF DARWIN}
  LIBYS = 'libys.dylib.0.2.29';
  {$ENDIF}
  {$IFDEF WINDOWS}
  LIBYS = 'libys.dll';
  {$ENDIF}

{ Create a new GraalVM isolate }
function graal_create_isolate(params: Pointer; isolate: PPointer;
  isolate_thread: PPointer): cint; cdecl; external LIBYS;

{ Tear down a GraalVM isolate }
function graal_tear_down_isolate(isolate_thread: Pointer): cint;
  cdecl; external LIBYS;

{ Compile and eval a YAMLScript string, returning a JSON response }
function load_ys_to_json(isolate_thread: Pointer; ys: PAnsiChar): PAnsiChar;
  cdecl; external LIBYS;

implementation

end.
