/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */

package de.ust.skill.generator.sidl

import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import de.ust.skill.ir
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.EnumType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.Typedef
import de.ust.skill.ir.WithFields
import de.ust.skill.ir.WithInheritance

trait SIDLMaker extends GeneralOutputMaker {
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
    val out = files.open("specification.sidl")

    // simpler types
    val simpleSpec = (
      for (
        t ← IR.getEnums.asScala ++ IR.getTypedefs.asScala
      ) yield t match {
        case t : EnumType ⇒ s"""${comment(t)}enum ${t.getName.capital}${
          if (0 == t.getInstances.size) ";"
          else s" ::= ${t.getInstances.asScala.mkString(" | ")}"
        }
${
          if (0 != t.getFields.size) {
            s"${t.getName.capital} -> ${mkFields(t)}"
          } else ""
        }
"""
        case t : Typedef ⇒ s"""${comment(t)}typedef ${t.getName.capital}
  ${
          t.getRestrictions.asScala.map(s ⇒ s"$s\n  ").mkString
        }${
          mapType(t.getTarget)
        };

"""
      }).mkString("\n\n")

    // types with inheritance
    val types = locally {
      // flip super type relation including interfaces
      val subtypes = new HashMap[WithInheritance, HashSet[Declaration with WithInheritance]]
      for (t ← IR.getInterfaces.asScala ++ IR.getUsertypes.asScala) {
        t.getSuperType match {
          case s : WithInheritance ⇒ subtypes.getOrElseUpdate(s, new HashSet) += t
          case _                   ⇒ // nothing todo
        }
        for (s ← t.getSuperInterfaces.asScala)
          subtypes.getOrElseUpdate(s, new HashSet) += t
      }

      (
        for (
          t ← IR.getInterfaces.asScala ++ IR.getUsertypes.asScala;
          subs = subtypes.getOrElse(t, HashSet.empty).toArray.sortBy(_.getName);
          keyword = if (t.isInstanceOf[InterfaceType]) "interface " else "";
          fields = mkFields(t)
        ) yield {
          val prefix = comment(t) + (t.getHints.asScala ++ t.getRestrictions.asScala).map(s ⇒ s"$s\n").mkString + keyword
          if (prefix.isEmpty && subs.isEmpty && fields.isEmpty &&
            (null != t.getSuperType || !t.getSuperInterfaces.isEmpty())) {
            // do not create useless redefinition
            ""
          } else {
            if (fields.isEmpty()) {
              s"""$prefix${t.getName.capital}${
                if (subs.isEmpty) ";"
                else s" ::= ${subs.map(_.getName.capital).mkString(" | ")}"
              }"""
            } else {
              s"""${
                if (prefix.isEmpty() && subs.isEmpty) ""
                else {
                  s"""$prefix${t.getName.capital}${
                    if (subs.isEmpty) ";"
                    else s" ::= ${subs.map(_.getName.capital).mkString(" | ")}"
                  }
"""
                }
              }${t.getName.capital} -> $fields"""
            }
          }
        }).mkString("\n\n")
    }

    if (types.isEmpty() || simpleSpec.isEmpty()) {
      out.write(simpleSpec + types)
    } else {
      out.write(simpleSpec)
      out.write("\n\n")
      out.write(types)
    }

    out.close()
  }

  private def mkFields(fs : Declaration with WithFields) : String = (for (
    f ← fs.getFields.asScala ++ fs.getViews.asScala ++ fs.getCustomizations.asScala
  ) yield f match {
    case f : ir.Field ⇒ {
      var prefix = comment(f) + (f.getHints.asScala ++ f.getRestrictions.asScala).map(s ⇒ s"$s\n  ").mkString
      // add a blank line before complex field specifications
      if (!prefix.isEmpty()) prefix = "\n  " + prefix
      else prefix = "  "

      if (f.isConstant()) s"""
${prefix}${f.getName.camel} = ${f.constantValue()} : const ${
        if (f.isAuto()) "auto "
        else ""
      }${mapType(f.getType)}"""
      else s"""
${prefix}${f.getName.camel} : ${
        if (f.isAuto()) "auto "
        else ""
      }${mapType(f.getType)}"""
    }

    case f : ir.View ⇒ {
      var prefix = comment(f)
      // add a blank line before complex field specifications
      if (!prefix.isEmpty()) prefix = "\n  " + prefix
      else prefix = "  "

      s"""
${prefix}${f.getName.camel} : ${mapType(f.getType)} view ${f.getTarget.getDeclaredIn.getName.camel}.${f.getTarget.getName.camel}"""
    }

    case f : ir.LanguageCustomization ⇒ s"""

  ${comment(f)}custom ${f.language}${
      (for ((n, p) ← f.getOptions.asScala) yield {
        s"""
  !$n${
          if (p.isEmpty()) ""
          else if (1 == p.size()) s""" "${p.get(0)}""""
          else p.asScala.mkString("(\"", "\" \"", "\")")
        }"""
      }).mkString
    }
  "${f.`type`}" ${f.getName.camel}"""
  }).mkString(",")
}

