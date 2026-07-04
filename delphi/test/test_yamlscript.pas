{ Copyright 2023-2026 Ingy dot Net
  This code is licensed under MIT license (See License for details) }

{ Test the yamlscript Delphi/Pascal binding. }

program test_yamlscript;

{$mode objfpc}{$H+}

uses
  SysUtils, fpjson, yamlscript;

var
  Fails: Integer = 0;

procedure Test(const Name: string; Cond: Boolean);
begin
  if Cond then
    WriteLn('ok - ', Name)
  else
  begin
    WriteLn('not ok - ', Name);
    Inc(Fails);
  end;
end;

procedure RunTests;
var
  ys: TYAMLScript;
  data: TJSONData;
  obj: TJSONObject;
  threw: Boolean;
begin
  ys := TYAMLScript.Create;
  try
    { Load YS code }
    data := ys.Load('!ys-0:' + LineEnding + 'test:: inc(41)');
    try
      obj := data as TJSONObject;
      Test('load ys code', obj.Integers['test'] = 42);
    finally
      data.Free;
    end;

    { Load plain YAML }
    data := ys.Load('foo: bar');
    try
      obj := data as TJSONObject;
      Test('load plain yaml', obj.Strings['foo'] = 'bar');
    finally
      data.Free;
    end;

    { Load invalid input raises }
    threw := False;
    try
      data := ys.Load(':');
      data.Free;
    except
      on EYAMLScriptException do
        threw := True;
    end;
    Test('load error raises', threw);

    { Load multiple times on one instance }
    data := ys.Load('!ys-0:' + LineEnding + 'test:: inc(41)');
    try
      obj := data as TJSONObject;
      Test('load multiple times', obj.Integers['test'] = 42);
    finally
      data.Free;
    end;
  finally
    ys.Free;
  end;
end;

begin
  RunTests;
  if Fails > 0 then
  begin
    WriteLn(Fails, ' test(s) failed');
    Halt(1);
  end;
end.
