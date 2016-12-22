package de.ust.skill.generator.haskell

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Field
import de.ust.skill.ir.UserType
import de.ust.skill.ir.Type
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SingleBaseTypeContainer

/**
 * creates the package interface
 */
trait TypesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    val out = open("Types.hs")

    out.write(s"""
module Types where

import qualified Data.Map as M
import Data.Int
import Memory
import Methods
${IR.map(declareType).mkString}
${IR.map(declareFields).mkString}
""")

    out.close()
  }

  private def declareType(t : UserType) : String = s"""${
    if (!t.getSubTypes.isEmpty) {
      val n = name(t)
      s"""
data $n' = $n'$n $n${
        allSubtypes(t).map(s ⇒ s" | $n'${name(s)} ${name(s)}").mkString
      } deriving (Show, Eq)"""
    } else ""
  }
type ${name(t)} = ${t.getAllFields.map(f ⇒ mapType(f.getType, false)).mkString("(", ", ", ")")}"""

  private def allSubtypes(t : UserType) : Seq[UserType] = (
    for (sub ← t.getSubTypes) yield Seq(sub) ++ allSubtypes(sub)
  ).flatten

  private def declareFields(t : UserType) : String = {
    val n = name(t)
    (for (f ← t.getAllFields) yield {
      val c_name = s"c'${n}_${name(f)}"
      val d_name = s"d'${n}_${name(f)}"

      f.getType match {
        case t : SingleBaseTypeContainer ⇒ locally {
          val typename = t match {
            case t : ConstantLengthArrayType ⇒ "GFArray"
            case t : VariableLengthArrayType ⇒ "GVArray"
            case t : ListType                ⇒ "GList"
            case t : SetType                 ⇒ "GSet"
          }
          s"""
$c_name vs = $typename $$ Prelude.map ${BoxedDataConstructor(t.getBaseType)} vs
$d_name ($typename vs) = Prelude.map (\\(${BoxedDataConstructor(t.getBaseType)} v) -> v) vs
"""
        }

        case t : MapType ⇒ locally {
          val ts = t.getBaseTypes.map(BoxedDataConstructor)
          val ls = t.getBaseTypes.map(BoxedDataConstructor).map(s ⇒ s"(\\($s v) -> v)")
          s"""
$c_name = ${ts.init.foldRight[String](ts.last) { case (k, v) ⇒ s"box_map $k ($v)" }}
$d_name = ${ls.init.foldRight[String](ls.last) { case (k, v) ⇒ s"unbox_map $k ($v)" }}
"""
        }

        case t ⇒ s"""
$c_name = ${BoxedDataConstructor(t)}
$d_name value (${BoxedDataConstructor(t)} value) = value
"""
      }
    }).mkString
  }
}