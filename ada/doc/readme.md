## Ada Usage

Use `YAMLScript.Load` to load YAML or YAMLScript text through `libys`.

```ada
with Ada.Text_IO;
with YAMLScript;

procedure Main is
   Data : constant String := YAMLScript.Load ("!ys-0" & ASCII.LF &
     "inc: 41");
begin
   Ada.Text_IO.Put_Line (Data);
end Main;
```


## Installation

Install the package and the `libys` shared library:

```bash
curl -sSL https://yamlscript.org/install | LIB=1 bash
export LD_LIBRARY_PATH="$HOME/.local/lib:$LD_LIBRARY_PATH"
```
