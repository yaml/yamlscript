with Ada.Strings.Fixed;
with Ada.Text_IO;
with YAMLScript;

procedure Test_YAMLScript is
   package Fixed renames Ada.Strings.Fixed;

   Fails : Natural := 0;

   procedure Check (Cond : Boolean; Label : String) is
   begin
      if Cond then
         Ada.Text_IO.Put_Line ("ok - " & Label);
      else
         Ada.Text_IO.Put_Line ("not ok - " & Label);
         Fails := Fails + 1;
      end if;
   end Check;

   JSON : constant String := YAMLScript.Load_JSON
     ("!ys-0:" & ASCII.LF & "test:: inc(41)");
begin
   Check (Fixed.Index (JSON, """test"":42") > 0, "load ys code");

   if Fails > 0 then
      raise Program_Error with "Ada YAMLScript tests failed";
   end if;
end Test_YAMLScript;
