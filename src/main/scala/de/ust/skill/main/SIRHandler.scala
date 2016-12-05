package de.ust.skill.main

import java.io.File
import java.io.IOException

import scala.collection.JavaConversions._
import scala.language.reflectiveCalls

import de.ust.skill.common.scala.api.Create
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.Write
import de.ust.skill.ir
import de.ust.skill.sir
import de.ust.skill.ir.TypeContext
import de.ust.skill.sir.api.SkillFile
import scala.collection.mutable.HashMap
import de.ust.skill.ir.GroundType
import java.io.PrintStream
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer
import de.ust.skill.common.scala.api.ThrowException

/**
 * Handler for a skill intermediate representation state.
 *
 * @author Timm Felden
 */
class SIRHandler private (val sf : SkillFile) {

  def this(path : String) {
    this(SkillFile.open(path, Read, Write))
  }

  def this(tc : TypeContext, path : String) {
    this{
      try {
        new File(path).delete()
      } catch {
        case e : IOException ⇒ // not important
      }
      SkillFile.open(path, Create, Write)
    }

    // copy type context to file
    buildIRFrom(tc)
  }

  //@note assume sf to be empty
  private val identifiers = new HashMap[String, sir.Identifier]
  private def convert(t : ir.Type) : sir.Identifier = {
    identifiers.getOrElseUpdate(t.getSkillName, sf.Identifier.make(
      parts = (t match {
        case t : ir.Declaration ⇒ t.getName
        case t : ir.GroundType  ⇒ t.getName
      }).parts.to,
      skillname = t.getSkillName
    ))
  }
  private def convert(f : ir.FieldLike) : sir.Identifier = {
    identifiers.getOrElseUpdate(f.getSkillName, sf.Identifier.make(
      parts = f.getName.parts.to,
      skillname = f.getSkillName
    ))
  }
  private def mkComment(c : ir.Comment) : sir.Comment = {
    if (c.isInstanceOf[ir.Comment.NoComment]) null
    else sf.Comment.make(
      text = c.getText.to,
      tags = c.getTags.map(t ⇒ sf.CommentTag.make(name = t.name, text = t.getText.to)).to
    )
  }

  private def buildIRFrom(tc : TypeContext) {

    // create types
    val typeMap = {
      // base types
      val base = tc.types.filterNot(_._2.isInstanceOf[ir.ContainerType]).map {
        case (name, t : ir.GroundType)    ⇒ (name, sf.SimpleType.make(name = convert(t)))
        case (name, t : ir.UserType)      ⇒ (name, sf.ClassType.make(name = convert(t), comment = mkComment(t.getComment)))
        case (name, t : ir.InterfaceType) ⇒ (name, sf.InterfaceType.make(name = convert(t), comment = mkComment(t.getComment)))

        case (name, t)                    ⇒ throw new Exception(s"forgot class ${t.getClass.getName}")
      }.toMap

      // container types
      (base.toSeq ++ tc.types.filter(_._2.isInstanceOf[ir.ContainerType]).map {
        case (name, t : ir.ConstantLengthArrayType) ⇒ (name, sf.ConstantLengthArrayType.make(
          base = base(t.getBaseType.getSkillName),
          kind = "array",
          length = t.getLength
        ))

        case (name, t : ir.VariableLengthArrayType) ⇒ (name, sf.SingleBaseTypeContainer.make(
          base = base(t.getBaseType.getSkillName),
          kind = "array"
        ))

        case (name, t : ir.ListType) ⇒ (name, sf.SingleBaseTypeContainer.make(
          base = base(t.getBaseType.getSkillName),
          kind = "list"
        ))

        case (name, t : ir.SetType) ⇒ (name, sf.SingleBaseTypeContainer.make(
          base = base(t.getBaseType.getSkillName),
          kind = "set"
        ))

        case (name, t : ir.MapType) ⇒ (name, sf.MapType.make(
          base = t.getBaseTypes.map(x ⇒ base(x.getSkillName)).to
        ))

        case (name, t) ⇒ throw new Exception(s"forgot class ${t.getClass.getName}")
      }).toMap
    }

    @inline
    def mapType(t : ir.Type) : sir.Type = typeMap(t.getSkillName)

    def mapField(f : ir.FieldLike) : sir.FieldLike = {
      val r = f match {
        case f : ir.Field ⇒ sf.Field.make(
          isAuto = f.isAuto,
          comment = mkComment(f.getComment),
          name = convert(f),
          `type` = mapType(f.getType)
        )
      }

      r
    }

    // copy properties
    tc.types.foreach {
      case (name, t : ir.UserType) ⇒ locally {
        val target = typeMap(name).asInstanceOf[sir.ClassType]

        target.`super` =
          if (null == t.getSuperType) null
          else typeMap(t.getSuperType.getSkillName).asInstanceOf[sir.ClassType]

        target.interfaces = t.getSuperInterfaces.map(i ⇒ typeMap(i.getSkillName).asInstanceOf[sir.InterfaceType]).to

        target.fields = t.getFields.map(mapField).to
      }
      case _ ⇒ // done
    }
  }

  object build {
    def apply(tool : sir.Tool) {
      println(s"building ${tool.name}...")

      // create temporary skill file for regular command line invocation
      val spec = File.createTempFile("spec", ".skill")
      //spec.deleteOnExit()
      try {
        @inline
        def extensions(t : {
                         def `super` : sir.ClassType;
                         def interfaces : HashSet[sir.InterfaceType]
                       }) : String = {
          (if (null == t.`super`) ""
          else s" extends ${name(t.`super`)}"
          ) + t.interfaces.map { x ⇒ s" with ${name(x)}" }.mkString("")
        }

        @inline
        def comment(c : sir.Comment) : String =
          if (null == c) ""
          else s"""/* ${c.text.mkString(" ")} ${
            c.tags.map {
              x ⇒ s"@${x.name} ${x.text.mkString(" ")}"
            }.mkString(" ")
          }*/"""

        @inline
        def hints(hs : Seq[sir.Hint]) : String = hs.map {
          h ⇒ s"!${h.name}${h.arguments.mkString("(", ",", ")")}"
        }.mkString("\n")

        @inline
        def restrictions(hs : Seq[sir.Restriction]) : String = hs.map {
          h ⇒ s"@${h.name}${h.arguments.mkString("(", ",", ")")}"
        }.mkString("\n")

        def fields(t : { def fields : ArrayBuffer[sir.FieldLike] }) : String = {
          t.fields.map {
            case x : sir.Field ⇒ s"""
  ${comment(x.comment)}
  ${hints(x.hints)}
  ${restrictions(x.restrictions)}
  ${mapType(x.`type`)} ${name(x)};
"""
            case x ⇒ """
  ???
"""
          }.mkString
        }

        val out = new PrintStream(spec)
        tool.selectedUserTypes.foreach {
          case x : sir.ClassType ⇒ out.println(s"""
${comment(x.comment)}
${hints(x.hints)}
${restrictions(x.restrictions)}
${name(x)}${extensions(x)} {${fields(x)}}""")

          case x : sir.InterfaceType ⇒ out.println(s"""
${comment(x.comment)}
${hints(x.hints)}
${restrictions(x.restrictions)}
interface ${name(x)}${extensions(x)} {${fields(x)}}""")

          case x ⇒ out.println(s"""
☢☢☢""")
        }

        out.close
      } catch {
        case e : IOException ⇒ throw new Exception("failed to create temporary specification", e)
      }

      // invoke generator for targets
      for (target ← tool.buildTargets) {
        val args = Array[String](spec.getAbsolutePath, "-o", mkPath(target.output), "-L", target.language) ++ target.options
        CommandLine.main(args)
      }
    }

    private def mkPath(path : sir.FilePath) : String = {
      path.parts.foldLeft(
        if (path.isAbsolut) new File("/")
        else new File("."))((f, e) ⇒ new File(f, e)
        ).getAbsolutePath
    }

    private def name(named : { def name : sir.Identifier }) : String = {
      val name = named.name
      return name.parts.mkString("_")
    }

    private def mapType(t : sir.Type) : String = t match {
      case null ⇒ "<<null in IR!>>"
      case t : sir.GroundType ⇒ name(t)
      case t : sir.ConstantLengthArrayType ⇒ s"${mapType(t.base)}[${t.length}]"
      case t : sir.SingleBaseTypeContainer if t.kind.equals("array") ⇒ s"${mapType(t.base)}[]"
      case t : sir.SingleBaseTypeContainer ⇒ s"${t.kind}<${mapType(t.base)}>"
      case t : sir.MapType ⇒ t.base.map(mapType).mkString("map<", ",", ">")
    }
  }
}