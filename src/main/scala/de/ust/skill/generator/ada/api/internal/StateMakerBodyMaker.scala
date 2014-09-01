/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait StateMakerBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-state_maker.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Api.Internal.State_Maker is

   procedure Create (State : access Skill_State) is
      Types : access Types_Hash_Map.Map := State.Types;
   begin
${
  /**
   * Puts all known missing types into the types hash map.
   */
  var output = "";
  for (d ← IR) {
    output += s"""      if not Types.Contains ("${d.getSkillName}") then
         declare
            Type_Name    : String := "${d.getSkillName}";
            Super_Name   : String := "${if (null == d.getSuperType) "" else d.getSuperType}";
            Fields       : Fields_Vector.Vector;
            Storage_Pool : Storage_Pool_Vector.Vector;
            New_Type     : Type_Information := new Type_Declaration'(
               Type_Size    => Type_Name'Length,
               Super_Size   => Super_Name'Length,
               id           => Long (Natural (Types.Length) + 32),
               Name         => Type_Name,
               Super_Name   => Super_Name,
               spsi         => 1,
               lbpsi        => 1,
               Fields       => Fields,
               Storage_Pool => Storage_Pool,
               Known        => True,
               Written      => False
            );
         begin
            Types.Insert (New_Type.Name, New_Type);
         end;
      else
         Types.Element ("${d.getSkillName}").Known := True;
      end if;\r\n\r\n"""
  }
  output.stripLineEnd
}
${
  /**
   * Puts all known missing fields into the field vector of a given type.
   */
  var output = "";
  for (d ← IR) {
     output += d.getFields.filter({ f ⇒ !f.isAuto && !f.isIgnored }).map({ f ⇒
       s"""      if not Has_Field (Types.Element ("${d.getSkillName}"), "${f.getSkillName}") then
         declare
            Type_Name  : String := "${d.getSkillName}";
            Field_Name : String := "${f.getSkillName}";
            Base_Types : Base_Types_Vector.Vector;
            New_Field  : Field_Information := new Field_Declaration'(
               Size                  => Field_Name'Length,
               Name                  => Field_Name,
               F_Type                => ${mapTypeToId(f.getType, f)},
               Constant_Value        => ${f.constantValue},
               Constant_Array_Length => ${
  f.getType match {
    case t: ConstantLengthArrayType ⇒ t.getLength
    case _ => 0
  }
},
               Base_Types            => Base_Types,
               Known                 => True,
               Written               => False
            );
         begin
${
  var output = ""
  f.getType match {
    case t: ConstantLengthArrayType ⇒
      output += s"            New_Field.Base_Types.Append (${mapTypeToId(t.getBaseType, f)});\r\n"
    case t: VariableLengthArrayType ⇒
      output += s"            New_Field.Base_Types.Append (${mapTypeToId(t.getBaseType, f)});\r\n"
    case t: ListType ⇒
      output += s"            New_Field.Base_Types.Append (${mapTypeToId(t.getBaseType, f)});\r\n"
    case t: SetType ⇒
      output += s"            New_Field.Base_Types.Append (${mapTypeToId(t.getBaseType, f)});\r\n"
    case t: MapType ⇒
      t.getBaseTypes.foreach({ t =>
        output += s"            New_Field.Base_Types.Append (${mapTypeToId(t, f)});\r\n"
      })
    case _ ⇒ null
  }
  output
}            Types.Element (Type_Name).Fields.Append (New_Field);
         end;
      else
         Get_Field (Types.Element ("${d.getSkillName}"), "${f.getSkillName}").Known := True;
      end if;\r\n\r\n"""}).mkString("")
  }
  output.stripLineEnd.stripLineEnd
}
   end Create;

   function Has_Field (
      Type_Declaration : Type_Information;
      Field_Name       : String
   ) return Boolean is
      use Fields_Vector;

      Position : Cursor := Type_Declaration.Fields.First;
   begin
      while Position /= No_Element loop
         declare
            Index : Positive := To_Index (Position);
         begin
            if Field_Name = Type_Declaration.Fields.Element (Index).Name then
               return True;
            end if;
         end;
         Next (Position);
      end loop;
      return False;
   end Has_Field;

   function Get_Field (
      Type_Declaration : Type_Information;
      Field_Name       : String
   ) return Field_Information is
      use Fields_Vector;

      Position : Cursor := Type_Declaration.Fields.First;
   begin
      while Position /= No_Element loop
         declare
            Index : Positive := To_Index (Position);
         begin
            if Field_Name = Type_Declaration.Fields.Element (Index).Name then
               return Type_Declaration.Fields.Element (Index);
            end if;
         end;
         Next (Position);
      end loop;
      return null;
   end Get_Field;

end ${packagePrefix.capitalize}.Api.Internal.State_Maker;
""")

    out.close()
  }
}
