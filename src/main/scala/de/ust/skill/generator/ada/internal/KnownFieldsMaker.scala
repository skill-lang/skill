/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait KnownFieldsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (
      t ← IR.par;
      f ← t.getFields.par
    ) {
      makeSpec(t, f)
      makeBody(t, f)
    }
  }

  private final def makeSpec(t : UserType, f : Field) {

    val out = open(s"""${packagePrefix}-known_field_${escaped(t.getName.ada).toLowerCase}_${escaped(f.getName.ada).toLowerCase}.ads""")

    val fn = s"${escaped(t.getName.ada)}_${escaped(f.getName.ada)}"

    out.write(s"""
with Skill.Files;
with Skill.Field_Declarations;
with Skill.Field_Types;
limited with ${poolsPackage};

package ${PackagePrefix}.Known_Field_$fn is

   type Known_Field_${fn}_T is
     new Skill.Field_Declarations.Field_Declaration_T with private;
   type Known_Field_$fn is access Known_Field_${fn}_T'Class;

   function Make
     (ID    : Natural;
      T     : Skill.Field_Types.Field_Type;
      Owner : Skill.Field_Declarations.Owner_T)
      return Skill.Field_Declarations.Field_Declaration;

   overriding
   procedure Free (This : access Known_Field_${fn}_T);

   function Owner_Dyn
     (This : access Known_Field_${fn}_T)
      return ${poolsPackage}.${name(t)}_P.Pool;

   overriding
   procedure Read
     (This : access Known_Field_${fn}_T;
      CE   : Skill.Field_Declarations.Chunk_Entry);

private

   type Known_Field_${fn}_T is new Skill.Field_Declarations
     .Field_Declaration_T with
   record
      null;
   end record;

end ${PackagePrefix}.Known_Field_$fn;
""")

    out.close()
  }

  private final def makeBody(t : UserType, f : Field) {

    val out = open(s"""${packagePrefix}-known_field_${escaped(t.getName.ada).toLowerCase}_${escaped(f.getName.ada).toLowerCase}.adb""")

    val fn = s"${escaped(t.getName.ada)}_${escaped(f.getName.ada)}"

    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Files;
with Skill.Field_Declarations;
with Skill.Field_Types;
with Skill.Internal.Parts;
with Skill.Streams.Reader;
with Skill.Types;
with $poolsPackage;

with ${PackagePrefix}.Internal_Skill_Names;

package body ${PackagePrefix}.Known_Field_$fn is

   function Make
     (ID    : Natural;
      T     : Skill.Field_Types.Field_Type;
      Owner : Skill.Field_Declarations.Owner_T)
      return Skill.Field_Declarations.Field_Declaration
   is
   begin
      return new Known_Field_${fn}_T'
          (Data_Chunks => Skill.Field_Declarations.Chunk_List_P.Empty_Vector,
           T           => T,
           Name        => ${internalSkillName(f)},
           Index       => ID,
           Owner       => Owner);
   end Make;

   procedure Free (This : access Known_Field_${fn}_T) is
      type P is access all Known_Field_${fn}_T;

      procedure Delete is new Ada.Unchecked_Deallocation
        (Known_Field_${fn}_T,
         P);
      D : P := P (This);
   begin
      This.Data_Chunks.Foreach (Skill.Field_Declarations.Delete_Chunk'Access);
      This.Data_Chunks.Free;
      Delete (D);
   end Free;

   function Owner_Dyn
     (This : access Known_Field_${fn}_T)
      return ${poolsPackage}.${name(t)}_P.Pool
   is
      function Cast is new Ada.Unchecked_Conversion
        (Skill.Field_Declarations.Owner_T,
         ${poolsPackage}.${name(t)}_P.Pool);
   begin
      return Cast (This.Owner);
   end Owner_Dyn;

   procedure Read
     (This : access Known_Field_${fn}_T;
      CE   : Skill.Field_Declarations.Chunk_Entry)
   is
      First : Natural;
      Last  : Natural;
      Data  : Skill.Types.Annotation_Array    := Owner_Dyn (This).Data;
      Input : Skill.Streams.Reader.Sub_Stream := CE.Input;
   begin
      if CE.C.all in Skill.Internal.Parts.Simple_Chunk then
         First := Natural (CE.C.To_Simple.BPO);
         Last  := First + Natural (CE.C.Count);
      else
         First := Natural (This.Owner.Blocks.Last_Element.BPO);
         Last  := First + Natural (This.Owner.Blocks.Last_Element.Count);
         -- TODO This is horribly incorrect!!!
      end if;

      for I in First + 1 .. Last loop
         To_${name(t)} (Data (I)).Set_${name(f)} (${read(f)});
      end loop;
   end Read;
end ${PackagePrefix}.Known_Field_$fn;
""")

    out.close()
  }

  private def read(f : Field) : String = f.getType match {
    case t : UserType ⇒ "null"
    case t : GroundType ⇒ t.getName.ada match {
      case "Annotation" ⇒ "null"
      case "String"     ⇒ "null"
      case n            ⇒ "Input."+n
    }
    case _ ⇒ "null"
  }
}
