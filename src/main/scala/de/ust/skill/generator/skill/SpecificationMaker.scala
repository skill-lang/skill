/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter

import de.ust.skill.ir.EnumType
import de.ust.skill.ir.Field
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.LanguageCustomization
import de.ust.skill.ir.Typedef
import de.ust.skill.ir.UserType

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
    }

    // write specification
    val out = files.open("specification.skill")

    out.write(s"""${
      (
        for (
          name ← IR.allTypeNames.asScala;
          t = IR.get(name)
        ) yield t match {
          case t : UserType ⇒ s"""${comment(t)}${t.getName.capital} ${
            if (null == t.getSuperType) ""
            else s": ${t.getSuperType.getName.capital} "
          }${
            t.getSuperInterfaces.asScala.map(s ⇒ s"with ${s.getName.capital} ").mkString
          }{${mkFields(t.getFields.asScala.to)}${mkCustom(t.getCustomizations.asScala.to)}
}

"""
          case t : InterfaceType ⇒ s"""${comment(t)}interface ${t.getName.capital} ${
            if (t.getSuperType.getSkillName == "annotation") ""
            else s": ${t.getSuperType.getName.capital} "
          }${
            t.getSuperInterfaces.asScala.map(s ⇒ s"with ${s.getName.capital} ").mkString
          }{${mkFields(t.getFields.asScala.to)}${mkCustom(t.getCustomizations.asScala.to)}
}

"""
          case t : EnumType ⇒ s"""${comment(t)}enum ${t.getName.capital} {
${t.getInstances.asScala.mkString("  ", ",\n  ", ";")}

${mkFields(t.getFields.asScala.to)}
}

"""
          case t : Typedef ⇒ s"""${comment(t)}typedef ${t.getName.capital}
  ${
            t.getRestrictions.asScala.map(s ⇒ s"$s\n  ").mkString
          }${
            mapType(t.getTarget)
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
    f.getRestrictions.asScala.map(s ⇒ s"$s\n  ").mkString
  }${mapType(f.getType)} ${f.getName.camel};""").mkString("\n")

  private def mkCustom(fs : List[LanguageCustomization]) : String = (for (f ← fs)
    yield s"""
  ${comment(f)}custom ${f.language}${
    (for ((k, v) ← f.getOptions.asScala)
      yield s"""
  !$k ${
      val vs = v.asScala.map(s ⇒ s""""$s"""")
      if (vs.size == 1) vs.head
      else vs.mkString("(", " ", ")")
    }""").mkString
  }
  "${f.`type`}" ${f.getName.camel};""").mkString("\n")
}
