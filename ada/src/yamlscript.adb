with Ada.Characters.Latin_1;
with Interfaces.C;
with Interfaces.C.Strings;
with System;

package body YAMLScript is
   package C renames Interfaces.C;
   package CS renames Interfaces.C.Strings;

   use type C.int;
   use type CS.chars_ptr;

   subtype Isolate_Thread is System.Address;

   function Create_Isolate
     (Params : System.Address;
      Isolate : System.Address;
      Thread : access Isolate_Thread) return C.int
      with Import, Convention => C, External_Name => "graal_create_isolate";

   function Tear_Down_Isolate
     (Thread : Isolate_Thread) return C.int
      with Import, Convention => C,
      External_Name => "graal_tear_down_isolate";

   function Load_YS_To_JSON
     (Thread : Isolate_Thread; Input : CS.chars_ptr) return CS.chars_ptr
      with Import, Convention => C, External_Name => "load_ys_to_json";

   function Load_JSON (Input : String) return String is
      Thread : aliased Isolate_Thread := System.Null_Address;
      C_Input : CS.chars_ptr := CS.New_String (Input);
      C_Output : CS.chars_ptr;
      RC : C.int;
   begin
      RC := Create_Isolate
        (System.Null_Address, System.Null_Address, Thread'Access);
      if RC /= 0 then
         CS.Free (C_Input);
         raise Program_Error with "Failed to create isolate";
      end if;

      C_Output := Load_YS_To_JSON (Thread, C_Input);
      CS.Free (C_Input);

      if C_Output = CS.Null_Ptr then
         RC := Tear_Down_Isolate (Thread);
         raise Program_Error with "Null response from libys";
      end if;

      declare
         Result : constant String := CS.Value (C_Output);
      begin
         RC := Tear_Down_Isolate (Thread);
         if RC /= 0 then
            raise Program_Error with "Failed to tear down isolate";
         end if;
         return Result;
      end;
   end Load_JSON;
end YAMLScript;
