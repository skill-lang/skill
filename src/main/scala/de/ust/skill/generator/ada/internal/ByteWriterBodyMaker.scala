/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait ByteWriterBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-byte_writer.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.Byte_Writer is

   procedure Write_Buffer (Stream : not null access Ada.Streams.Root_Stream_Type'Class; Item : in Buffer) is
      use Ada.Streams;

      Buffer : Stream_Element_Array (1 .. Stream_Element_Offset (Buffer_Index));
      Last : Stream_Element_Offset := Stream_Element_Offset (Buffer_Index);
   begin
      for I in 1 .. Last loop
         Buffer (I) := Stream_Element (Item (Integer (I)));
      end loop;

      Stream.Write (Buffer);
   end Write_Buffer;

   procedure Finalize_Buffer (Stream : ASS_IO.Stream_Access) is
   begin
      Buffer'Write (Stream, Buffer_Array);
      Buffer_Index := 0;
   end;

   procedure Write_Byte (Stream : ASS_IO.Stream_Access; Next : Byte) is
   begin
      Buffer_Index := Buffer_Index + 1;
      Buffer_Array (Buffer_Index) := Next;

      if Buffer_Size = Buffer_Index then
         Buffer'Write (Stream, Buffer_Array);
         Buffer_Index := 0;
      end if;
   end Write_Byte;

   procedure Write_i8 (Stream : ASS_IO.Stream_Access; Value : i8) is
      function Convert is new Ada.Unchecked_Conversion (Source => i8, Target => Byte);
   begin
      Write_Byte (Stream, Convert (Value));
   end Write_i8;

   procedure Write_i16 (Stream : ASS_IO.Stream_Access; Value : i16) is
      type Result is new Interfaces.Unsigned_16;
      function Convert is new Ada.Unchecked_Conversion (Source => i16, Target => Result);

      A : Result := (Convert (Value) / (2 ** 8)) and 16#ff#;
      B : Result := Convert (Value) and 16#ff#;
   begin
      Write_Byte (Stream, Byte (A));
      Write_Byte (Stream, Byte (B));
   end Write_i16;

   procedure Write_i32 (Stream : ASS_IO.Stream_Access; Value : i32) is
      type Result is new Interfaces.Unsigned_32;
      function Convert is new Ada.Unchecked_Conversion (Source => i32, Target => Result);

      A : Result := (Convert (Value) / (2 ** 24)) and 16#ff#;
      B : Result := (Convert (Value) / (2 ** 16)) and 16#ff#;
      C : Result := (Convert (Value) / (2 ** 8)) and 16#ff#;
      D : Result := Convert (Value) and 16#ff#;
   begin
      Write_Byte (Stream, Byte (A));
      Write_Byte (Stream, Byte (B));
      Write_Byte (Stream, Byte (C));
      Write_Byte (Stream, Byte (D));
   end Write_i32;

   procedure Write_i64 (Stream : ASS_IO.Stream_Access; Value : i64) is
      type Result is new Interfaces.Unsigned_64;
      function Convert is new Ada.Unchecked_Conversion (Source => i64, Target => Result);

      A : Result := (Convert (Value) / (2 ** 56)) and 16#ff#;
      B : Result := (Convert (Value) / (2 ** 48)) and 16#ff#;
      C : Result := (Convert (Value) / (2 ** 40)) and 16#ff#;
      D : Result := (Convert (Value) / (2 ** 32)) and 16#ff#;
      E : Result := (Convert (Value) / (2 ** 24)) and 16#ff#;
      F : Result := (Convert (Value) / (2 ** 16)) and 16#ff#;
      G : Result := (Convert (Value) / (2 ** 8)) and 16#ff#;
      H : Result := Convert (Value) and 16#ff#;
   begin
      Write_Byte (Stream, Byte (A));
      Write_Byte (Stream, Byte (B));
      Write_Byte (Stream, Byte (C));
      Write_Byte (Stream, Byte (D));
      Write_Byte (Stream, Byte (E));
      Write_Byte (Stream, Byte (F));
      Write_Byte (Stream, Byte (G));
      Write_Byte (Stream, Byte (H));
   end Write_i64;

   --  optimized write v64: taken from the scala binding
   procedure Write_v64 (Stream : ASS_IO.Stream_Access; Value : v64) is
      use Interfaces;

      subtype Result is Long range 0 .. 63;
      Index : Result;
      function Convert is new Ada.Unchecked_Conversion (Source => v64, Target => Unsigned_64);
   begin

      System.Machine_Code.Asm ("bsr %1, %0",
         Outputs => Long'Asm_Output ("=a", Index),
         Inputs  => Long'Asm_Input ("a", Value)
         --  Volatile => True
      );

      case Index is
         when 0 .. 6 =>
            declare
               A : Unsigned_64 := Convert (Value) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
            end;

         when 7 .. 13 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (Convert (Value) / (2 ** 7)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
            end;

         when 14 .. 20 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (Convert (Value) / (2 ** 14)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
            end;

         when 21 .. 27 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 14))) and 16#ff#;
               D : Unsigned_64 := (Convert (Value) / (2 ** 21)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
               Write_Byte (Stream, Byte (D));
            end;

         when 28 .. 34 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 14))) and 16#ff#;
               D : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 21))) and 16#ff#;
               E : Unsigned_64 := (Convert (Value) / (2 ** 28)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
               Write_Byte (Stream, Byte (D));
               Write_Byte (Stream, Byte (E));
            end;

         when 35 .. 41 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 14))) and 16#ff#;
               D : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 21))) and 16#ff#;
               E : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 28))) and 16#ff#;
               F : Unsigned_64 := (Convert (Value) / (2 ** 35)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
               Write_Byte (Stream, Byte (D));
               Write_Byte (Stream, Byte (E));
               Write_Byte (Stream, Byte (F));
            end;

         when 42 .. 48 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 14))) and 16#ff#;
               D : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 21))) and 16#ff#;
               E : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 28))) and 16#ff#;
               F : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 35))) and 16#ff#;
               G : Unsigned_64 := (Convert (Value) / (2 ** 42)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
               Write_Byte (Stream, Byte (D));
               Write_Byte (Stream, Byte (E));
               Write_Byte (Stream, Byte (F));
               Write_Byte (Stream, Byte (G));
            end;


         when 49 .. 55 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 14))) and 16#ff#;
               D : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 21))) and 16#ff#;
               E : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 28))) and 16#ff#;
               F : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 35))) and 16#ff#;
               G : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 42))) and 16#ff#;
               H : Unsigned_64 := (Convert (Value) / (2 ** 49)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
               Write_Byte (Stream, Byte (D));
               Write_Byte (Stream, Byte (E));
               Write_Byte (Stream, Byte (F));
               Write_Byte (Stream, Byte (G));
               Write_Byte (Stream, Byte (H));
            end;

         when 56 .. 63 =>
            declare
               A : Unsigned_64 := (16#80# or Convert (Value)) and 16#ff#;
               B : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 7))) and 16#ff#;
               C : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 14))) and 16#ff#;
               D : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 21))) and 16#ff#;
               E : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 28))) and 16#ff#;
               F : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 35))) and 16#ff#;
               G : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 42))) and 16#ff#;
               H : Unsigned_64 := (16#80# or (Convert (Value) / (2 ** 49))) and 16#ff#;
               I : Unsigned_64 := (Convert (Value) / (2 ** 56)) and 16#ff#;
            begin
               Write_Byte (Stream, Byte (A));
               Write_Byte (Stream, Byte (B));
               Write_Byte (Stream, Byte (C));
               Write_Byte (Stream, Byte (D));
               Write_Byte (Stream, Byte (E));
               Write_Byte (Stream, Byte (F));
               Write_Byte (Stream, Byte (G));
               Write_Byte (Stream, Byte (H));
               Write_Byte (Stream, Byte (I));
            end;
      end case;
   end Write_v64;

   procedure Write_Boolean (Stream : ASS_IO.Stream_Access; Value : Boolean) is
   begin
      case Value is
         when True => Write_Byte (Stream, 16#ff#);
         when False => Write_Byte (Stream, 16#00#);
      end case;
   end Write_Boolean;

   procedure Write_String (Stream : ASS_IO.Stream_Access; Value : String) is
   begin
      for I in Value'Range loop
         Write_Byte (Stream, Byte (Character'Pos (Value (I))));
      end loop;
   end Write_String;

end ${packagePrefix.capitalize}.Internal.Byte_Writer;
""")

    out.close()
  }
}
