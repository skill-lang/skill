/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import de.ust.skill.ir.UserType
import scala.collection.JavaConversions._
import de.ust.skill.ir.Typedef
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.EnumType
import scala.annotation.tailrec
import de.ust.skill.ir.Field
/**
 * Creates user type equivalents.
 *
 * @author Timm Felden
 */
trait SpecificationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // drop types
    var IR = tc;
    for (d ← droppedKinds) d match {
      case Interfaces ⇒ IR = IR.removeInterfaces()
      case Enums      ⇒ IR = IR.removeEnums()
      case Typedefs   ⇒ IR = IR.removeTypedefs()
      case Views      ⇒ IR = IR.removeViews()
    }

    // write specification
    val out = open("specification.skill")

    out.write(s"""${
      (
        for (
          name ← IR.allTypeNames;
          t = IR.get(name)
        ) yield t match {
          case t : UserType ⇒ s"""${comment(t)}${t.getName.capital} ${
            if (null == t.getSuperType) ""
            else s": ${t.getSuperType.getName.capital} "
          }${
            t.getSuperInterfaces.map(s ⇒ s"with ${s.getName.capital} ").mkString
          }{${mkFields(t.getFields.to)}
}

"""
          case t : InterfaceType ⇒ s"""${comment(t)}interface ${t.getName.capital} ${
            if (t.getSuperType.getSkillName == "annotation") ""
            else s": ${t.getSuperType.getName.capital} "
          }${
            t.getSuperInterfaces.map(s ⇒ s"with ${s.getName.capital} ").mkString
          }{${mkFields(t.getFields.to)}
}

"""
          case t : EnumType ⇒ s"""${comment(t)}enum ${t.getName.capital} {
${t.getInstances.mkString("  ", ",\n  ", ";")}

${mkFields(t.getFields.to)}
}

"""
          case t : Typedef ⇒ s"""${comment(t)}typedef ${t.getName.capital} 
  ${
            t.getRestrictions.map(s ⇒ s"$s\n  ").mkString
          }${
            t.getTarget.getName.capital
          };

"""
          case _ ⇒ "" // no action required (built-in type)
        }
      ).mkString
    }""")

    out.close()
  }

  private def mkFields(fs : List[Field]) : String = (for (f ← fs)
    yield s"""
  ${comment(f)}${
    f.getRestrictions.map(s ⇒ s"$s\n  ").mkString
  }${f.getType} ${f.getName.camel};"""
  ).mkString("\n")
}
