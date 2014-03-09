/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import scala.collection.JavaConversions._
import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker

trait StateMakerBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-state_maker.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.State_Maker is

   procedure Create (State : access Skill_State) is
   begin
${
  var output = "";
  for (d ← IR) {
    output += s"""      if not State.Has_Type ("${d.getSkillName}") then
         declare
            Type_Name : String := "${d.getSkillName}";
            Super_Name : String := "${if (null == d.getSuperType) "" else d.getSuperType}";
            Fields : Fields_Vector.Vector;
            Storage_Pool : Storage_Pool_Vector.Vector;
            New_Type : Type_Information := new Type_Declaration'(
               Type_Size => Type_Name'Length,
               Super_Size => Super_Name'Length,
               id => State.Type_Size + 32,
               Name => Type_Name,
               Super_Name => Super_Name,
               bpsi => 1,
               lbpsi => 1,
               Fields => Fields,
               Storage_Pool => Storage_Pool
            );
         begin
            State.Put_Type (New_Type);
         end;
      end if;\r\n\r\n"""
  }
  output.stripSuffix("\r\n")
}
${
  var output = "";
  for (d ← IR) {
     output += d.getFields.filter({ f ⇒ !f.isAuto && !f.isIgnored }).map({ f ⇒
       s"""      if not State.Has_Field ("${d.getSkillName}", "${f.getSkillName}") then
         declare
            Type_Name : String := "${d.getSkillName}";
            Field_Name : String := "${f.getSkillName}";
            New_Field : Field_Information := new Field_Declaration'(
               Size => Field_Name'Length,
               Name => Field_Name,
               F_Type => ${mapTypeToId(f.getType, f)},
               Constant_Value => ${f.constantValue},
               Constant_Array_Length => ${
  f.getType match {
    case t: ConstantLengthArrayType ⇒ t.getLength
    case _ => -1
  }
},
               Base_Type => ${
  f.getType match {
    case t: ConstantLengthArrayType ⇒ mapTypeToId(t.getBaseType, f)
    case t: VariableLengthArrayType ⇒ mapTypeToId(t.getBaseType, f)
    case t: ListType ⇒ mapTypeToId(t.getBaseType, f)
    case t: SetType ⇒ mapTypeToId(t.getBaseType, f)
    case _ ⇒ -1
  }
}
            );
         begin
            State.Put_Field (Type_Name, New_Field);
         end;
      end if;\r\n\r\n"""}).mkString("")
  }
  output.stripSuffix("\r\n\r\n")
}
   end Create;

end ${packagePrefix.capitalize}.Internal.State_Maker;
""")

    out.close()
  }
}
