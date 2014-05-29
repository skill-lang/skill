/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait FileReaderSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-file_reader.ads""")

    out.write(s"""
private with ${packagePrefix.capitalize}.Api.Internal.Byte_Reader;

with Ada.Unchecked_Deallocation;

--
--  This package reads a given .sf file in following order until the end of file:
--
--  1. string pool
--    a) it reads all the strings from the string block of the file and puts them into the string pool
--
--  2. type block
--    a) it reads the type declaration name
--      -> if the type is not present in the types hashmap, it will read the type information and insert it into the types hashmap
--    b) it reads the super type and if necessary, the local base pool start index
--    c) it reads the instance counter
--    d) it reads the field counter
--    e) depending on the instance and field counter, it will continue with one of the three cases:
--      -> case 1: if instance counter = 0 then assume new fields
--      -> case 2: if instance counter > 0 and field counter = 0 then assume new objects
--      -> case 3: if instance counter > 0 and field counter > 0 then assume new fields and after that new objects
--      => when new fields are read, the field will be put into a queue
--    f) it reads the field data processing the queue from start to end
--
--  3. update storage pool start index
--    -> this is necessary to ensure that new instances will get the next correct skill-id
--

package ${packagePrefix.capitalize}.Api.Internal.File_Reader is

   procedure Read (
      State     : access Skill_State;
      File_Name : String
   );

private

   Input_File : ASS_IO.File_Type;
   Input_Stream : ASS_IO.Stream_Access;

   type Queue_Item is
      record
         Type_Declaration  : Type_Information;
         Field_Declaration : Field_Information;
         Start_Index       : Natural;
         End_Index         : Natural;
         Data_Length       : Long;
      end record;

   package Read_Queue_Vector is new Ada.Containers.Vectors (Positive, Queue_Item);
   Read_Queue : Read_Queue_Vector.Vector;

   --------------------
   --  STRING BLOCK  --
   --------------------
   procedure Read_String_Block;

   ------------------
   --  TYPE BLOCK  --
   ------------------
   procedure Read_Type_Block;
   procedure Read_Type_Declaration (Last_End : in out Long);
   procedure Read_Field_Declaration (Type_Name : String);
   procedure Read_Field_Data;

   procedure Create_Objects (
      Type_Name      : String;
      Instance_Count : Natural
   );
   procedure Read_Queue_Vector_Iterator (Iterator : Read_Queue_Vector.Cursor);

   function Read_Annotation (Input_Stream : ASS_IO.Stream_Access) return Skill_Type_Access;
   function Read_String (Input_Stream : ASS_IO.Stream_Access) return String_Access;

${
  var output = ""
  for (d ← IR) {
    output += s"   function Read_${escaped(d.getName)}_Type (Input_Stream : ASS_IO.Stream_Access) return ${escaped(d.getName)}_Type_Access;\r\n"
  }
  output
}
   ------------
   --  SPSI  --
   ------------
   procedure Update_Storage_Pool_Start_Index;

   ------------
   --  SKIP  --
   ------------
   procedure Skip_Restrictions;

end ${packagePrefix.capitalize}.Api.Internal.File_Reader;
""")

    out.close()
  }
}
