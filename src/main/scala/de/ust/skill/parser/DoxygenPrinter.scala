package de.ust.skill.parser

import scala.collection.mutable.ArrayBuffer
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import de.ust.skill.parser._

/**
 * Debug pretty printer to check interpretation of specifications.
 * The output will allways be in $cwd/out.h.
 *
 * @author Timm Felden
 */
object DoxygenPrinter {

  def apply(defs : ArrayBuffer[Declaration]) {
    Files.write(Paths.get("out.h"), defs.map(pretty).mkString("\n").getBytes(StandardCharsets.UTF_8))
  }

  def pretty(d : Declaration) : String = d match {
    case t : UserType ⇒ s"""
${t.description.comment.map(s ⇒ "/**\n"+s.text.mkString+"\n*/").getOrElse("")}
class ${t.name.CapitalCase} ${
      if (t.superTypes.isEmpty) ""
      else t.superTypes.map(_.CapitalCase).mkString(": virtual protected ", ", virtual protected ", "")
    }{
  public:${
      (
        for (f ← t.body)
          yield s"""

${f.description.comment.map { s ⇒ "    /**\n"+s.text.mkString.replace('<', '⟨').replace('>', '⟩')+"\n*/" }.getOrElse("")}
   ${mapType(f.t)} ${f.name.camelCase};"""
      ).mkString
    }
};
"""
    case t : InterfaceDefinition ⇒ s"""
${t.comment.map(s ⇒ "/**\n"+s.text.mkString+"\n*/").getOrElse("")}
class ${t.name.CapitalCase} ${
      if (t.superTypes.isEmpty) ""
      else t.superTypes.map(_.CapitalCase).mkString(": virtual protected ", ", virtual protected ", "")
    }{
  public:${
      (
        for (f ← t.body)
          yield s"""

${f.description.comment.map(s ⇒ "    /**\n"+s.text.mkString+"\n*/").getOrElse("")}
   ${mapType(f.t)} ${f.name.camelCase};"""
      ).mkString
    }
};
"""
    case t : EnumDefinition ⇒ s"""
${t.comment.map(s ⇒ "/**\n"+s.text.mkString+"\n*/").getOrElse("")}
enum ${t.name.CapitalCase} {
  ${t.instances.map { i ⇒ i.camelCase }.mkString(", ")}
};
"""
    case t : Typedef ⇒ s"""
typedef ${mapType(t.target)} ${t.name.CapitalCase};
"""
  }

  def mapType(t : Type) : String = t match {
    case t : ListType                ⇒ s"std::list<${mapType(t.baseType)}>"
    case t : SetType                 ⇒ s"std::set<${mapType(t.baseType)}>"
    case t : MapType                 ⇒ t.baseTypes.map(mapType).reduceRight { (b, r) ⇒ s"std::map<$b, $r>" }
    case t : ConstantLengthArrayType ⇒ s"${t.baseType}[${t.length}]"
    case t : ArrayType               ⇒ s"${t.baseType}[]"
    case t : BaseType                ⇒ t.name.CapitalCase
  }
  //${f.description.comment.map(s ⇒ "/**\n"+s.text.mkString.replace("<", "[").replace(">", "]").replace("*/, "* /")+"\n*/").getOrElse("")}
}