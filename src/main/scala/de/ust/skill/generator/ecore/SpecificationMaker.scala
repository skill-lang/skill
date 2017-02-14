/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ecore

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.MapType
import de.ust.skill.ir.Name
import scala.collection.mutable.HashMap

/**
 * Creates user type equivalents.
 *
 * @author Timm Felden
 */
trait SpecificationMaker extends GeneralOutputMaker {

  // ecoreName -> ecoreRepresentation
  private var pendingMapTypes = new HashMap[String, String]

  abstract override def make {
    super.make

    // write specification
    val out = files.open("specification.ecore")

    out.write(s"""<?xml version="1.0" encoding="UTF-8"?>
<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="${packagePrefix}" nsURI="test" nsPrefix="eiml">""")

    for (t ← tc.removeSpecialDeclarations().getUsertypes) {
      out.write(s"""
  <eClassifiers xsi:type="ecore:EClass" name="${mapType(t)}"${
        if (null != t.getSuperType) s""" eSuperTypes="#//${mapType(t.getSuperType)}""""
        else ""
      }>
    ${
        t.getFields.map(mkField).mkString("\n")
      }
  </eClassifiers>""")
    }
    pendingMapTypes.values.foreach(out.write)
    out.write("""
</ecore:EPackage>
""")

    out.close()
  }

  private def mkFields(fs : List[Field]) : String = (for (f ← fs)
    yield s"""
  ${comment(f)}${
    f.getRestrictions.map(s ⇒ s"$s\n  ").mkString
  }${mapType(f.getType)} ${f.getName.camel};"""
  ).mkString("\n")

  def mkField(f : Field) : String = {
    var suffix = if (f.isAuto()) """ transient="true""""
    else ""

    f.getType match {
      case t : SingleBaseTypeContainer ⇒ mkField(t.getBaseType, f.getName.camel, """ upperBound="-1"""" + suffix);
      case t : MapType ⇒ locally {
        val kt = t.getBaseTypes.get(0)
        val vt = t.getBaseTypes.get(1)
        val mapName = s"${mapType(kt)}To${mapType(vt)}Map"
        pendingMapTypes.getOrElseUpdate(mapName, s"""
<eClassifiers xsi:type="ecore:EClass" name="$mapName" instanceClassName="java.util.Map$$Entry">
${mkField(kt, "key", "")}
${mkField(vt, "value", "")}
</eClassifiers>""")
        s"""<eStructuralFeatures xsi:type="ecore:EReference" containment="true" upperBound="-1" name="${
          f.getName.camel
        }" eType="#//$mapName"/>"""
      }
      case t ⇒ mkField(t, f.getName.camel, suffix);
    }
  }

  def mkField(t : Type, name : String, suffix : String) : String = {
    s"""<eStructuralFeatures xsi:type="ecore:E${
      if (t.getSkillName.equals("annotation") || t.isInstanceOf[UserType]) "Reference"
      else "Attribute"
    }" name="$name" eType="${
      mapBaseType(t)
    }"$suffix/>"""
  }

  private def mapBaseType(t : Type) : String = t match {
    case ft : GroundType if ft.getSkillName.equals("annotation") ⇒ "ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"
    case ft : GroundType ⇒ "ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//" + (ft.getSkillName match {
      case "bool"        ⇒ "EBoolean"
      case "i8"          ⇒ "EByte"
      case "i16"         ⇒ "EShort"
      case "i32"         ⇒ "EInt"
      case "i64" | "v64" ⇒ "ELong"
      case "f32"         ⇒ "EFloat"
      case "f64"         ⇒ "EDouble"
      case "string"      ⇒ "EString"
    })
    case t : UserType ⇒ s"#//${mapType(t)}"
  }
}
