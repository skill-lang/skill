/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign

import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.HashMap

import de.ust.skill.generator.jforeign.api.SkillFileMaker
import de.ust.skill.generator.jforeign.internal.AccessMaker
import de.ust.skill.generator.jforeign.internal.FieldDeclarationMaker
import de.ust.skill.generator.jforeign.internal.FileParserMaker
import de.ust.skill.generator.jforeign.internal.StateMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.jforeign.IRMapper
import de.ust.skill.jforeign.mapping.MappingParser
import de.ust.skill.jforeign.typing.TypeChecker
import de.ust.skill.main.HeaderInfo

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker {
  def make {
    initialize(this.asInstanceOf[Main], types)
  }

  /** Runner for java-foreign specific stuff. */
  def initialize(generator : Main, skillTc : TypeContext) {
    if (null == mappingFile) {
      throw new IllegalStateException("a mapping file must be provided to java foreign via -Ojavaforeign:m=<path>")
    }

    // parse mapping
    val mappingParser = new MappingParser()
    val mappingRules = mappingParser.process(new FileReader(generator.getMappingFile()))
    // get list of java class names that we want to map
    val javaTypeNames = mappingRules.map { _.getJavaTypeName }
    // map
    val mapper = new IRMapper(generator.getForeignSources())
    val (javaTc, rc) = mapper.mapClasses(javaTypeNames)
    // bind and typecheck
    val typeRules = mappingRules.flatMap { r ⇒ r.bind(skillTc, javaTc) }
    val checker = new TypeChecker
    val (_, mappedFields) = checker.check(typeRules, skillTc, javaTc, rc)
    // remove unmapped fields
    for (ut ← javaTc.getUsertypes) {
      val fieldsToDelete : List[Field] = ut.getFields.filterNot { f ⇒ mappedFields.contains(f) }.toList
      ut.getFields.removeAll(fieldsToDelete)
    }
    // generate specification file if requested
    generator.getGenSpecPath.foreach { path ⇒
      val f = new File(path)
      val prettySkillSpec = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f)))
      javaTc.getUsertypes.foreach { ut ⇒
        prettySkillSpec.write(ut.prettyPrint() + "\n")
      }
      prettySkillSpec.close()
    }
    // prepare generator
    generator.setForeignTC(javaTc)
    generator.setReflectionContext(rc)
  }
}

/**
 * A generator turns a set of skill declarations into a Java interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with AccessMaker
    with FieldDeclarationMaker
    with FileParserMaker
    with StateMaker
    with SkillFileMaker
    with InterfacesMaker
    with DependenciesMaker
    with SubTypeMaker
    with AspectMaker {

  lineLength = 120
  override def comment(d : Declaration) : String = d.getComment.format("/**\n", " * ", lineLength, " */\n")
  override def comment(f : FieldLike) : String = f.getComment.format("/**\n", "     * ", lineLength, "     */\n    ")

  /**
   * Translates types into scala type names.
   */
  override protected def mapType(t : Type, boxed : Boolean) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "de.ust.skill.common.jforeign.internal.SkillObject"

      case "bool"       ⇒ if (boxed) "java.lang.Boolean" else "boolean"

      case "i8"         ⇒ if (boxed) "java.lang.Byte" else "byte"
      case "i16"        ⇒ if (boxed) "java.lang.Short" else "short"
      case "i32"        ⇒ if (boxed) "java.lang.Integer" else "int"
      case "i64"        ⇒ if (boxed) "java.lang.Long" else "long"
      case "v64"        ⇒ if (boxed) "java.lang.Long" else "long"

      case "f32"        ⇒ if (boxed) "java.lang.Float" else "float"
      case "f64"        ⇒ if (boxed) "java.lang.Double" else "double"

      case "string"     ⇒ "java.lang.String"
    }

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName<${mapType(t.getBaseType(), true)}>"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName<${mapType(t.getBaseType(), true)}>"
    case t : ListType                ⇒ s"$ListTypeName<${mapType(t.getBaseType(), true)}>"
    case t : SetType                 ⇒ s"$SetTypeName<${mapType(t.getBaseType(), true)}>"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType(_, true)).reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")

    case t : Declaration             ⇒ userPackagePrefix(t) + name(t)

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  override protected def mapType(f : Field, boxed : Boolean) : String = f.getType match {
    case t : ListType ⇒ s"${rc.map(f).getName}<${mapType(t.getBaseType(), true)}>"
    case t : SetType  ⇒ s"${rc.map(f).getName}<${mapType(t.getBaseType(), true)}>"
    case t : MapType  ⇒ t.getBaseTypes().map(mapType(_, true)).reduceRight((k, v) ⇒ s"${rc.map(f).getName}<$k, $v>")
    case _            ⇒ mapType(f.getType, boxed)
  }

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  override protected def makeConstructorArguments(t : UserType) = (
    for (f ← t.getAllFields if !(f.isConstant || f.isIgnored))
      yield s"${mapType(f, false)} ${name(f)}").mkString(", ")

  override protected def appendConstructorArguments(t : UserType, prependTypes : Boolean) = {
    val r = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if (r.isEmpty) ""
    else if (prependTypes) r.map({ f ⇒ s", ${mapType(f.getType())} ${name(f)}" }).mkString("")
    else r.map({ f ⇒ s", ${name(f)}" }).mkString("")
  }

  override def makeHeader(headerInfo : HeaderInfo) : String = headerInfo.format(this, "/*", "*\\", " *", "* ", "\\*", "*/")

  /**
   * provides the package prefix
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_ + "." + _)
  }

  override def packageDependentPathPostfix = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
  override def defaultCleanMode = "file";

  override def setOption(option : String, value : String) : Unit = option match {
    case "revealskillid"    ⇒ revealSkillID = ("true" == value);
    case "suppresswarnings" ⇒ suppressWarnings = if ("true" == value) "@SuppressWarnings(\"all\")\n" else ""
    case "m"                ⇒ mappingFile = value
    case "f"                ⇒ foreignSources += value
    case "genspec"          ⇒ genSpecPath = Some(value)
    case unknown            ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def helpText : String = """
revealSkillID     true/false  if set to true, the generated binding will reveal SKilL IDs in the API
suppressWarnings  true/false  add a @SuppressWarnings("all") annotation to generated classes
m                 <path>      mapping file which ties SKilL types to Java types
f                 <path>      class path from where Java types are looked up. May be specified multiple times.
genspec           <path>      generate SKilL specification from foreign types
"""

  override def customFieldManual : String = """
!import string+    A list of imports that will be added where required.
!modifier string   A modifier, that will be put in front of the variable declaration."""

  override protected def defaultValue(f : Field) = f.getType match {
    case t : GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "null"
    }

    case _ ⇒ "null"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  private val escapeCache = new HashMap[String, String]();
  final def escaped(target : String) : String = escapeCache.getOrElse(target, {
    val result = target match {
      //keywords get a suffix "_", because that way at least auto-completion will work as expected
      case "abstract" | "continue" | "for" | "new" | "switch" | "assert" | "default" | "if" | "package" | "synchronized"
        | "boolean" | "do" | "goto" | "private" | "this" | "break" | "double" | "implements" | "protected" | "throw"
        | "byte" | "else" | "import" | "public" | "throws" | "case" | "enum" | "instanceof" | "return" | "transient"
        | "catch" | "extends" | "int" | "short" | "try" | "char" | "final" | "interface" | "static" | "void" | "class"
        | "finally" | "long" | "strictfp" | "volatile" | "const" | "float" | "native" | "super" | "while" ⇒ target + "_"

      //the string is fine anyway
      case _ ⇒ target.map {
        case ':'                                    ⇒ "$"
        case c if Character.isJavaIdentifierPart(c) ⇒ c.toString
        case c                                      ⇒ "ZZ" + c.toHexString
      }.reduce(_ + _)
    }
    escapeCache(target) = result
    result
  })
}
