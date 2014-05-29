/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait StateMakerSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-state_maker.ads""")

    out.write(s"""
--
--  This package puts all known user types into the types hashmap, if it not
--  already contains it. All known fields will be put into the field vector
--  of a type, if it not already contains it.
--  It will be called in the API procedures Create and Read at the end.
--

package ${packagePrefix.capitalize}.Api.Internal.State_Maker is

   procedure Create (State : access Skill_State);

private

   function Has_Field (
      Type_Declaration : Type_Information;
      Field_Name       : String
   ) return Boolean;

   function Get_Field (
      Type_Declaration : Type_Information;
      Field_Name       : String
   ) return Field_Information;

end ${packagePrefix.capitalize}.Api.Internal.State_Maker;
""")

    out.close()
  }
}
