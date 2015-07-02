/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada2

import scala.collection.JavaConversions._

trait DynamicArraySpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""dynamic-array.adb""")

    out.write(s"""
with Ada.Unchecked_Deallocation;

package body Dynamic_Array is

   procedure Append (
      Container   : in out Vector;
      New_Element : Element_Type
   ) is
   begin
      Container.Ensure_Size (1 + Container.Size_0);
      Container.Append_Unsafe (New_Element);
   end Append;

   procedure Append_Unsafe (
      Container   : in out Vector;
      New_Element : Element_Type
   ) is
   begin
      Container.Size_0 := 1 + Container.Size_0;
      Container.Elements (Container.Size_0) := New_Element;
   end Append_Unsafe;

   function Element (
      Container : in out Vector;
      Index     : Index_Type
   ) return Element_Type is
   begin
      if (Index <= Container.Size_0) then
         return Container.Elements (Index);
      else
         raise Constraint_Error with "index check failed";
      end if;
   end Element;

   procedure Ensure_Size (
      Container : in out Vector;
      N         : Index_Type
   ) is
   begin
      if (N > Container.Elements'Length) then
         declare
            New_Size : Index_Type := 2 * Container.Size;
         begin
            while (N > New_Size) loop
               New_Size := 2 * New_Size;
            end loop;

            declare
               New_Container : Element_Array_Access := new Element_Array (1 .. New_Size);
               procedure Free is new Ada.Unchecked_Deallocation (Element_Array, Element_Array_Access);
            begin
               New_Container (1 .. Container.Size) := Container.Elements (1 .. Container.Size);
               Free (Container.Elements);
               Container.Elements := New_Container;
            end;

            Container.Size := New_Size;
         end;
      end if;
   end Ensure_Size;

   procedure Ensure_Allocation (
      Container : in out Vector;
      N         : Index_Type
   ) is
   begin
      Container.Ensure_Size (Container.Size_0 + N);
      Container.Size_0 := N + Container.Size_0;
   end Ensure_Allocation;

   function Length (
      Container : in out Vector
   ) return Index_Type is
   begin
      return Container.Size_0;
   end Length;

   procedure Replace_Element (
      Container : in out Vector;
      Index     : Index_Type;
      Element   : Element_Type
   ) is
   begin
      Container.Elements (Index) := Element;
   end Replace_Element;

   function Check_Index (
      Container : in out Vector;
      Index     : Index_Type
   ) return Boolean is
      (Index <= Container.Size_0);

   overriding
   procedure Initialize (Object : in out Vector) is
   begin
      Object.Elements := new Element_Array (1 .. Object.Size);
   end Initialize;

   overriding
   procedure Finalize (Object : in out Vector) is
   begin
      null;
   end Finalize;

end Dynamic_Array;
    """.replaceAll("""\h+$""", ""))

    out.close()
  }
}
