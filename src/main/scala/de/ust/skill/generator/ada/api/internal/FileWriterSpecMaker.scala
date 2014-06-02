/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait FileWriterSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-file_writer.ads""")

    out.write(s"""
private with ${packagePrefix.capitalize}.Api.Internal.Byte_Reader;
private with ${packagePrefix.capitalize}.Api.Internal.Byte_Writer;

with Ada.Tags;

--
--  This package writes a .sf file in the following order:
--
--  1. String pool
--    a) it collects all strings, it can find in following order:
--      go through all type declarations and
--        -> collect type name and super name
--        -> go through all field declarations and collect field name
--        -> go through storage pool and collect fields with type string (also in compound types)
--    b) it writes the string pool to the file
--
--  2. Type block
--    a) it ensures the type order of all storage pools
--      -> the code for this functionality will be only generated, if necessary
--    b) it writes all instantiated types
--      -> type and field declaration will be written to the file
--      -> field data will be written to a temporary stream
--    c) it copys the field data from the temporary stream to the file
--
--  3. Update storage pool start index (spsi)
--    -> this is necessary to ensure that new instances will get the next correct skill id
--

package ${packagePrefix.capitalize}.Api.Internal.File_Writer is

   procedure Append (
      State     : access Skill_State;
      File_Name : String
   );
   procedure Write (
      State     : access Skill_State;
      File_Name : String
   );

private

   type Modus_Type is (Append, Write);

   Field_Data_File : ASS_IO.File_Type;
   Field_Data_Stream : ASS_IO.Stream_Access;
   Output_Stream : ASS_IO.Stream_Access;

   procedure Run (Output_File : ASS_IO.File_Type);

   -------------------
   --  STRING BLOCK --
   -------------------
   procedure Prepare_String_Pool;
   procedure Prepare_String_Pool_Iterator (Iterator : Types_Hash_Map.Cursor);

   function Get_String_Index (Value : String) return Positive;
   procedure Put_String (
      Value : String;
      Safe : Boolean := False
   );
   procedure Write_String_Pool;

   ------------------
   --  TYPE BLOCK  --
   ------------------
   --  PHASE 1
   --  if subtypes are used, this procedure will ensure the type order of
   --  instantiated types.
   procedure Ensure_Type_Order;
   function Is_Type_Instantiated (Type_Declaration : Type_Information) return Boolean;
   function Count_Instantiated_Types return Long;

   --  PHASE 2
   procedure Write_Type_Block;
   function Count_Known_Fields (Type_Declaration : Type_Information) return Long;
   function Count_Known_Unwritten_Fields (Type_Declaration : Type_Information) return Long;
   procedure Write_Type_Declaration (Type_Declaration : Type_Information);
   procedure Write_Field_Declaration (
      Type_Declaration  : Type_Information;
      Field_Declaration : Field_Information
   );
   function Field_Data_Size (
      Type_Declaration  : Type_Information;
      Field_Declaration : Field_Information
   ) return Long;

   --  PHASE 3
   procedure Write_Field_Data (
      Stream            : ASS_IO.Stream_Access;
      Type_Declaration  : Type_Information;
      Field_Declaration : Field_Information
   );
   procedure Copy_Field_Data;

   ------------------------
   --  WRITE PROCEDURES  --
   ------------------------
   procedure Write_Annotation (
      Stream : ASS_IO.Stream_Access;
      Object : Skill_Type_Access
   );
   procedure Write_String (
      Stream : ASS_IO.Stream_Access;
      Value  : String_Access
   );

${
  var output = ""
  for (d ← IR) {
    output += s"""   procedure Write_${escaped(d.getName)}_Type (
      Stream : ASS_IO.Stream_Access;
      Object : ${escaped(d.getName)}_Type_Access
   );\r\n"""
  }
  output
}
   function Get_Object_Type (Object : Skill_Type_Access) return String;

   ------------
   --  SPSI  --
   ------------
   --  SPSI: Storage Pool Start Index
   --  Updates the spsi of each type to the next free skill id.
   procedure Update_Storage_Pool_Start_Index;

end ${packagePrefix.capitalize}.Api.Internal.File_Writer;
""")

    out.close()
  }
}
