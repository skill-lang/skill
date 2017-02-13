package de.ust.skill.generator.haskell

import scala.collection.JavaConversions._

import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType

/**
 * creates the package interface
 */
trait FollowMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    val out = files.open("Follow.hs")

    out.write(s"""module Follow where

import qualified Data.Map as M
import qualified Data.List as L
import Deserialize
import Memory
import Types
import Methods
""")

    for (t ← IR) {
      val n = name(t)

      val fields = t.getAllFields
      val range = (0 until fields.size)

      out.write(s"""
follow$n :: [TD] -> Ref -> Maybe $n${if (t.getSubTypes.isEmpty()) "" else "'"}
follow$n _ (_,-1)    = Nothing
follow$n tDs ref
${
        (for (sub ← allTypes(t)) yield {

          s"""  | name == "${sub.getSkillName}" = Just ${
            if (t.getSubTypes.isEmpty) ""
            else s"$$ $n'${name(sub)} "
          }(extract${name(sub)} pos fDs)"""
        }).mkString("\n")
      }
  | otherwise = error "can't identify target of reference"
      where (fDs, name, pos) = ref `reach` tDs

extract$n :: Int -> [FD] -> $n
extract$n i ${range.map(i ⇒ s"(n$i, f$i, _)").mkString("[", ", ", "]")}
           = ${
        fields.zipWithIndex.map {
          case (f, i) ⇒ s"(d'${n}_${name(f)}(f !! i$i))"
        }.mkString("(", ", ", ")")
      }
                 where f = map (!! i) ${range.map(i ⇒ s"f$i").mkString("[", ", ", "]")}
                       names = ${range.map(i ⇒ s"n$i").mkString("[", ", ", "]")}${
        fields.zipWithIndex.map {
          case (f, i) ⇒ s"""
                       i$i = assure $$ L.findIndex (== "${f.getSkillName}") names"""
        }.mkString
      }

override$n :: TD -> Int -> $n -> TD
override$n (TD e1 e2 e3 fDs e5 e6) index obj = TD e1 e2 e3 (go fDs index obj) e5 e6
  where go ${range.map(i ⇒ s"(n$i, d$i, r$i)").mkString("[", ", ", "]")} i ${range.map(i ⇒ s"o$i").mkString("(", ", ", ")")}
            = ${range.map(i ⇒ s"(n$i, (op !! i$i) d$i, r$i)").mkString("[", ", ", "]")}
               where op = map (r i) ${
        fields.zipWithIndex.map { case (f, i) ⇒ s"(c'${n}_${name(f)} o$i)" }.mkString("[", ", ", "]")
      }
                     names = ${fields.map(f ⇒ s""""${f.getSkillName}"""").mkString("[", ", ", "]")}${
        range.map(i ⇒ s"""
                     i$i = assure $$ L.findIndex (== n$i) names"""
        ).mkString
      }
""")
    }

    out.close()
  }

  private def allTypes(t : UserType) : Seq[UserType] = Seq[UserType](t) ++ t.getSubTypes.map(allTypes).flatten
}