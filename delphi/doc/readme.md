## Delphi / Pascal Usage

Use `yamlscript` as a drop-in replacement for your current YAML
loader:

File `main.pas`:

```pascal
program main;

{$mode objfpc}{$H+}

uses
  Classes, SysUtils, fpjson, yamlscript;

var
  ys: TYAMLScript;
  input: TStringList;
  data: TJSONData;
begin
  ys := TYAMLScript.Create;
  input := TStringList.Create;
  try
    input.LoadFromFile('config.yaml');
    data := ys.Load(input.Text);
    try
      WriteLn(data.FormatJSON);
    finally
      data.Free;
    end;
  finally
    input.Free;
    ys.Free;
  end;
end.
```


## Installation

Get the source units from the
[yamlscript-delphi](https://github.com/yaml/yamlscript-delphi) repo
and install the `libys.so` shared library:

```bash
git clone https://github.com/yaml/yamlscript-delphi
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```

Compile with the unit and library paths:

```bash
fpc -Fuyamlscript-delphi/src -Fl$HOME/.local/lib main.pas
```

See <https://yamlscript.org/doc/install/> for more info.


### Requirements

* Free Pascal Compiler (FPC) 3.0 or higher
* Linux, macOS or Windows
