/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter
import de.ust.skill.ir._
import de.ust.skill.ir.restriction.{FloatRangeRestriction, IntRangeRestriction, NonNullRestriction}

import scala.collection.JavaConversions.asScalaBuffer

trait PoolsMaker extends GeneralOutputMaker {
  final def makePools(out : PrintWriter) {

    // reflection has to know projected definitions
    val flatIR = this.types.removeSpecialDeclarations().getUsertypes

    for (t ← IR) {
      val isBasePool = null == t.getSuperType
      val nameT = name(t)
      val typeT = mapType(t)
      val accessT = access(t)
      var numberAutoFields = 0

      // find all fields that belong to the projected version, but use the unprojected variant
      val flatIRFieldNames = flatIR.find(_.getName == t.getName).get.getFields.map(_.getSkillName).toSet
      val fields = t.getAllFields.filter(f ⇒ flatIRFieldNames.contains(f.getSkillName))
      val projectedField = flatIR.find(_.getName == t.getName).get.getFields.map {
        case f ⇒ fields.find(_.getSkillName.equals(f.getSkillName)).get -> f
      }.toMap
      for (f ← fields if f.isAuto){numberAutoFields += 1}


      //class declaration
      out.write(s"""
class $accessT(${
        if (isBasePool) s"BasePool"
        else s"StoragePool"
      }):
${comment (t)}
    def __init__(self, poolIndex${
        if (isBasePool){""}
        else ", superPool"
      }, cls, subCls):
        \"\"\"
        Can only be constructed by the SkillFile in this package.
        \"\"\"
        super(${access(t)}, self).__init__(poolIndex, "${t.getSkillName}"${
          if (isBasePool) ""
          else ", superPool"
      }, ${
          if (fields.isEmpty) "[]"
          else fields.map { f ⇒ s""""${f.getSkillName}"""" }.mkString("[", ", ", "]")
      }, [None for i in range(0, $numberAutoFields)])
        self._cls = cls
        self._subCls = subCls
${
        if (fields.isEmpty) ""
        else s"""
    def addKnownField(self, name, string, annotation):${
          (for (f ← fields)
            yield
            if (f == fields.head)s"""
        if name == "${f.getSkillName}":
            ${knownField(projectedField(f))}(${mapToFieldType(f)}, self)
"""
            else s"""
        elif name == "${f.getSkillName}":
            ${knownField(projectedField(f))}(${mapToFieldType(f)}, self)
                """
          ).mkString
        }
    def addField(self, fType, name):${
          (for (f ← fields if !f.isAuto)
            yield
                s"""
        if name == "${f.getSkillName}":
            return ${knownField(projectedField(f))}(fType, self)
"""
          ).mkString
        }${
          (for (f ← fields if f.isAuto)
            yield
                if (f == fields.head)s"""
        if name == "${f.getSkillName}":
            raise SkillException(
                "The file contains a field declaration %s.%s, but there is an auto field of similar name!".format(
                    self.name(), name))"""
                else s"""
        elif name == "${f.getSkillName}":
            raise SkillException(
                "The file contains a field declaration %s.%s, but there is an auto field of similar name!".format(
                    self.name(), name))"""
          ).mkString
        }
        else:
            return LazyField(fType, name, self)"""
      }
${
        if (fields.forall { f ⇒ f.isConstant || f.isIgnored }) ""
        else s"""
    def make(self${appendInitializationArguments(t)}):
        \"\"\"
        :return a new $typeT instance with the argument field values
        \"\"\"
        rval = self._cls(-1${appendInitializationArguments(t, prependTypes = false)})
        self.add(rval)
        return rval
"""
      }
    def build(self):
        return self.${nameT}Builder(self, self._cls())

    class ${name(t)}Builder(StoragePool.Builder):
        \"\"\"
        Builder for new $nameT instances.
        \"\"\"
        ${
        (for (f ← t.getAllFields if !f.isIgnored && !f.isConstant)
          yield s"""
        def ${name(f)}(self, ${name(f)}):
            self.instance.set${escaped(f.getName.capital)}(${name(f)})
            return self
        """).mkString
      }
        def make(self):
            self.pool.add(self.instance)
            rval = self.instance
            self.instance = None
            return rval

    def makeSubPool(self, index, name):
        \"\"\"
        used internally for type forest construction
        \"\"\"
        return $accessT.UnknownSubPool(index, name, self, self._subCls)

    class UnknownSubPool(StoragePool):
        def __init__(self, poolIndex, name, superPools, subCls):
            super(${access(t)}.UnknownSubPool, self).__init__(poolIndex, name, superPools, self.noKnownFields, self.noAutoFields)
            self._subCls = subCls

        def makeSubPool(self, index, name):
            return type(self)(index, name, self)
""")
      }
    }

  protected def mapToFieldType(f : Field) : String = {
    //@note temporary string & annotation will be replaced later on
    @inline def mapGroundType(t : Type) : String = t.getSkillName match {
      case "annotation" ⇒ "annotation"
      case "bool"       ⇒ "BoolType()"
      case "i8"         ⇒ if (f.isConstant) s"ConstantI8(${f.constantValue})" else "I8()"
      case "i16"        ⇒ if (f.isConstant) s"ConstantI16(${f.constantValue})" else "I16()"
      case "i32"        ⇒ if (f.isConstant) s"ConstantI32(${f.constantValue})" else "I32()"
      case "i64"        ⇒ if (f.isConstant) s"ConstantI64(${f.constantValue})" else "I64()"
      case "v64"        ⇒ if (f.isConstant) s"ConstantV64(${f.constantValue})" else "V64()"
      case "f32"        ⇒ "F32()"
      case "f64"        ⇒ "F64()"
      case "string"     ⇒ "string"

      case _            ⇒ s"""self.owner().${name(t)}"""
    }

    f.getType match {
      case t : GroundType ⇒ mapGroundType(t)
      case t : ConstantLengthArrayType ⇒
        s"ConstantLengthArray(${t.getLength}, ${mapGroundType(t.getBaseType)})"

      case t : VariableLengthArrayType ⇒
        s"VariableLengthArray(${mapGroundType(t.getBaseType)})"

      case t : ListType ⇒
        s"ListType(${mapGroundType(t.getBaseType)})"

      case t : SetType ⇒
        s"SetType(${mapGroundType(t.getBaseType)})"

      case t : MapType ⇒
        t.getBaseTypes.map(mapGroundType).reduceRight((k, v) ⇒ s"MapType($k, $v)")

      case t ⇒ s"""self.owner().${name(t)}"""

    }
  }

}
