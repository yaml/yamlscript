{ Copyright 2023-2026 Ingy dot Net
  This code is licensed under MIT license (See License for details) }

{
  Delphi (Free Pascal) binding for the libys shared library.

  This unit is a Pascal port of the Python 'yamlscript' module, the
  reference implementation for YAMLScript FFI bindings to libys.

  The current user facing API consists of a single class, TYAMLScript,
  which has a single method: Load(string).
  The Load() method takes a YAMLScript string as input and returns the
  TJSONData object that the YAMLScript code evaluates to.
}

unit yamlscript;

{$mode objfpc}{$H+}

interface

uses
  SysUtils, fpjson, jsonscanner, jsonparser, yamlscript_native;

const
  { This value is automatically updated by 'make bump'.
    Version matching the libys shared library }
  YAMLSCRIPT_VERSION = '0.2.29';

type
  { Exception raised when YAMLScript encounters an error }
  EYAMLScriptException = class(Exception);

  { Interface to the libys shared library }
  TYAMLScript = class
  private
    FIsolateThread: Pointer;
    function ParseResponse(const JSONStr: string): TJSONData;
  public
    { Constructor - creates a new GraalVM isolate }
    constructor Create;

    { Destructor - tears down the GraalVM isolate }
    destructor Destroy; override;

    { Compile and eval a YAMLScript string and return the result.

      Args:
        Input: String containing YAMLScript (or plain YAML) content

      Returns:
        TJSONData object that the YAMLScript code evaluates to

      Raises:
        EYAMLScriptException if the input is invalid

      Note: Caller is responsible for freeing the returned TJSONData }
    function Load(const Input: string): TJSONData;
  end;

implementation

{ TYAMLScript }

constructor TYAMLScript.Create;
var
  rc: Integer;
begin
  inherited Create;
  FIsolateThread := nil;

  { Create a new GraalVM isolate }
  rc := yamlscript_native.graal_create_isolate(nil, nil, @FIsolateThread);

  if rc <> 0 then
    raise EYAMLScriptException.Create('Failed to create isolate');
end;

destructor TYAMLScript.Destroy;
var
  rc: Integer;
begin
  { Tear down the isolate thread to free resources }
  if FIsolateThread <> nil then
  begin
    rc := yamlscript_native.graal_tear_down_isolate(FIsolateThread);
    if rc <> 0 then
      raise EYAMLScriptException.Create('Failed to tear down isolate');
  end;

  inherited Destroy;
end;

function TYAMLScript.ParseResponse(const JSONStr: string): TJSONData;
var
  Parser: TJSONParser;
  Response: TJSONObject;
  ErrorObj: TJSONObject;
  ErrorMsg: string;
begin
  Result := nil;
  Parser := TJSONParser.Create(JSONStr, [joUTF8]);
  try
    Response := Parser.Parse as TJSONObject;
    try
      { Check for libys error in JSON response }
      if Response.Find('error') <> nil then
      begin
        ErrorObj := Response.Objects['error'];
        if ErrorObj.Find('cause') <> nil then
          ErrorMsg := ErrorObj.Strings['cause']
        else
          ErrorMsg := 'Unknown error from libys';
        raise EYAMLScriptException.Create(ErrorMsg);
      end;

      { Get the data field }
      if Response.Find('data') = nil then
        raise EYAMLScriptException.Create(
          'Unexpected response from libys');

      Result := Response.Extract('data');
    finally
      Response.Free;
    end;
  finally
    Parser.Free;
  end;
end;

function TYAMLScript.Load(const Input: string): TJSONData;
var
  JSONResponse: PAnsiChar;
  JSONStr: string;
begin
  { Call load_ys_to_json function in libys shared library }
  JSONResponse := yamlscript_native.load_ys_to_json(
    FIsolateThread, PAnsiChar(AnsiString(Input)));
  JSONStr := string(JSONResponse);

  { Parse and return the response }
  Result := ParseResponse(JSONStr);
end;

end.
