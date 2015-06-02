/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api.internal

import de.ust.skill.generator.ada.GeneralOutputMaker

trait PackageInternalSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal.ads""")

    out.write(s"""
with Ada.Streams.Stream_IO;
with Ada.Unchecked_Deallocation;
with Interfaces;

--
--  This package provides the functionality that will be used by the subpackages.
--

package ${packagePrefix.capitalize}.Api.Internal is

   package ASS_IO renames Ada.Streams.Stream_IO;

   type Byte is new Interfaces.Unsigned_8;
   for Byte'Size use 8;
   type Byte_Array is array (Positive range <>) of Byte;

   type Storage_Pool_Array is array (Natural range <>) of Skill_Type_Access;
   type Storage_Pool_Array_Access is access Storage_Pool_Array;

   procedure Free is new Ada.Unchecked_Deallocation (Storage_Pool_Array, Storage_Pool_Array_Access);

end ${packagePrefix.capitalize}.Api.Internal;
""")

    out.close()
  }
}
