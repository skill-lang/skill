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

   procedure Write_Byte (Stream : ASS_IO.Stream_Access; Next : Byte) is
   begin
      Byte'Write (Stream, Next);
   end Write_Byte;

   procedure Write_i8 (Stream : ASS_IO.Stream_Access; Value : i8) is
      function Convert is new Ada.Unchecked_Conversion (Source => i8, Target => Byte);
   begin
      Write_Byte (Stream, Convert (Value));
   end Write_i8;

   procedure Write_i16 (Stream : ASS_IO.Stream_Access; Value : i16) is
      type Result is mod 2 ** 16;
      function Convert is new Ada.Unchecked_Conversion (Source => i16, Target => Result);

      A : Result := (Convert (Value) / (2 ** 8)) and 16#ff#;
      B : Result := Convert (Value) and 16#ff#;
   begin
      Write_Byte (Stream, Byte (A));
      Write_Byte (Stream, Byte (B));
   end Write_i16;

   procedure Write_i32 (Stream : ASS_IO.Stream_Access; Value : i32) is
      type Result is mod 2 ** 32;
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
      type Result is mod 2 ** 64;
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

   procedure Write_v64 (Stream : ASS_IO.Stream_Access; Value : v64) is
      rval : Byte_v64_Type := Get_v64_Bytes (Value);
   begin
      for I in rval'Range loop
         Write_Byte (Stream, rval (I));
      end loop;
   end Write_v64;

   function Get_v64_Bytes (Value : v64) return Byte_v64_Type is
      Size : Natural := 0;
   begin
      declare
         type Result is mod 2 ** 64;
         function Convert is new Ada.Unchecked_Conversion (Source => v64, Target => Result);

         Buckets : Result := Convert (Value);
      begin
         while (Buckets > 0) loop
            Buckets := Buckets / (2 ** 7);
            Size := Size + 1;
         end loop;
      end;

      case Size is
         when 0 => return (0 => 0);
         when 10 => Size := 9;
         when others => null;
      end case;

      declare
         type Result is mod 2 ** 64;
         function Convert is new Ada.Unchecked_Conversion (Source => v64, Target => Result);

         rval : Byte_v64_Type (0 .. Size - 1);
         Count : Natural := 0;
      begin
         while (Count < 8 and then Count < Size - 1) loop
            rval (Count) := Byte (((Convert (Value) / (2 ** (7 * Count))) or 16#80#) and 16#ff#);
            Count := Count + 1;
         end loop;
         rval (Count) := Byte ((Convert (Value) / (2 ** (7 * Count))) and 16#ff#);
         return rval;
      end;
   end Get_v64_Bytes;

   procedure Write_f32 (Stream : ASS_IO.Stream_Access; Value : f32) is
      Skill_Unsupported_Type : exception;
   begin
      raise Skill_Unsupported_Type;
   end Write_f32;

   procedure Write_f64 (Stream : ASS_IO.Stream_Access; Value : f64) is
      Skill_Unsupported_Type : exception;
   begin
      raise Skill_Unsupported_Type;
   end Write_f64;

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
