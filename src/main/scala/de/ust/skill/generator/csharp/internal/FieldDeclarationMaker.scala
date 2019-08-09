/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp.internal

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

import de.ust.skill.generator.csharp.GeneralOutputMaker
import de.ust.skill.io.PrintWriter
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType

trait FieldDeclarationMaker extends GeneralOutputMaker {
  final def makeFields(out : PrintWriter) {

    // project IR, so that reflection implements the binary file format correctly
    val IR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR.asScala) {
      val autoFieldIndex : Map[Field, Int] = t.getFields.asScala.filter(_.isAuto()).zipWithIndex.toMap

      for (f ← t.getFields.asScala) {
        // the field before interface projection
        val originalF = this.types.removeTypedefs.removeEnums.get(t.getSkillName).asInstanceOf[UserType]
          .getAllFields.asScala.find(_.getName == f.getName).get

        // the type before the interface projection
        val fieldActualType = mapType(originalF.getType, true)
        val fieldActualTypeUnboxed = mapType(originalF.getType, false)

        val tIsBaseType = t.getSuperType == null

        val nameT = mapType(t)
        val nameF = knownField(f)

        // casting access to data array using index i
        val declareD = s"${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner.basePool()).Data;"
        val fieldAccess = (
          if (null == t.getSuperType) "d[i]"
          else s"((${mapType(t)})d[i])") + s".${name(f)}"

        out.write(s"""
        /// <summary>
        /// ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
        /// </summary>
        internal sealed class $nameF : ${
                  if (f.isAuto) "AutoField"
                  else "KnownDataField"
                }<$fieldActualType, ${mapType(t)}>${
                  var interfaces = new ArrayBuffer[String]()

                  // mark ignored fields as ignored; read function is inherited
                  if (f.isIgnored()) interfaces += "IgnoredField"

                  // mark interface fields
                  if (f.getType.isInstanceOf[InterfaceType]) interfaces += "InterfaceField"

                  if (interfaces.isEmpty) ""
                  else interfaces.mkString(", ", ", ", "")
                } {

            public $nameF(de.ust.skill.common.csharp.@internal.FieldType type, ${access(t)} owner) : base(type, "${f.getSkillName}"${
                  if (f.isAuto()) ", " + -autoFieldIndex(f)
                  else ""
                }, owner) {
                ${
                  if (f.isAuto()) "" // auto fields are always correctly typed
                  else s"""
                if (${
                    originalF.getType match {
                      case t : GroundType ⇒ s"type.TypeID != ${typeID(f.getType) - (if (f.isConstant()) 7 else 0)}"
                      case t : InterfaceType ⇒
                        if (t.getSuperType.getSkillName.equals("annotation")) "type.TypeID != 5"
                        else s"""!(type is IInterfacePool?((IInterfacePool)type).Name.Equals("${t.getSkillName}"):((AbstractStoragePool)(de.ust.skill.common.csharp.api.FieldType)type).Name.Equals("${f.getType.getSkillName}"))"""
                      case t : UserType ⇒ s"""!((AbstractStoragePool)(de.ust.skill.common.csharp.api.FieldType)type).Name.Equals("${f.getType.getSkillName}")"""

                      case _            ⇒ "false)//TODO type check!"
                    }
                  })
                    throw new SkillException("Expected field type ${f.getType.toString} in ${t.getName.capital}.${f.getName.camel} but found " + type);
        """
                }
                // TODO insert known restrictions?
            }
        ${
                  if (f.isAuto) ""
                  else if (f.isConstant) """
            public override void rsc(int i, int h, MappedInStream @in) {
            }
            public override void osc(int i, int h) {
            }
            public override void wsc(int i, int h, MappedOutStream @out) {
            }
"""
          else s"""
            public override void rsc(int i, int h, MappedInStream @in) {
                ${readCode(t, originalF)}
            }
            public override void osc(int i, int h) {${
                    val (code, isFast) = offsetCode(t, f, originalF.getType, fieldActualType);
                    if (isFast)
                      code
                    else s"""${prelude(originalF.getType)}
                ${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner.basePool).Data;
                long result = 0L;
                for (; i != h; i++) {
                    $code
                }
                offset += result;"""
                  }
            }
            public override void wsc(int i, int h, MappedOutStream @out) {
                ${writeCode(t, originalF)}
            }
"""
        }

            public override object get(SkillObject @ref) {
                ${
                  if (f.isConstant()) s"return ${mapType(t)}.${name(f)};"
                  else s"return ((${mapType(t)}) @ref).${name(f)};"
                }
            }

            public override void set(SkillObject @ref, object value) {
                ${
                  if (f.isConstant()) s"""throw new Exception("${f.getName.camel} is a constant!");"""
                  else if (originalF.getType.isInstanceOf[ListType]) s"((${mapType(t)}) @ref).${name(f)} = ((System.Collections.Generic.List<object>)value).Cast<${mapType(originalF.getType().asInstanceOf[ListType].getBaseType)}>().ToList();"
                  else if (originalF.getType.isInstanceOf[ConstantLengthArrayType]) s"((${mapType(t)}) @ref).${name(f)} = (System.Collections.ArrayList)value;"
                  else if (originalF.getType.isInstanceOf[VariableLengthArrayType]) s"((${mapType(t)}) @ref).${name(f)} = (System.Collections.ArrayList)value;"
                  else if (originalF.getType.isInstanceOf[SetType]) s"((${mapType(t)}) @ref).${name(f)} = new System.Collections.Generic.HashSet<${mapType(originalF.getType().asInstanceOf[SetType].getBaseType)}>(((System.Collections.Generic.HashSet<object>)value).Cast<${mapType(originalF.getType().asInstanceOf[SetType].getBaseType)}>());"
                  else if (originalF.getType.isInstanceOf[MapType]) s"((${mapType(t)}) @ref).${name(f)} = castMap<${mapType(originalF.getType.asInstanceOf[MapType].getBaseTypes().get(0))}, ${transformMapTypes(originalF.getType.asInstanceOf[MapType].getBaseTypes(), 1)}, object, object>((System.Collections.Generic.Dictionary<object, object>)value);"
                  else s"((${mapType(t)}) @ref).${name(f)} = ($fieldActualType)value;"
                }
            }
        }
""")
      }
    }
  }

  /**
   * create local variables holding type representants with correct type to help
   * the compiler
   */
  private final def prelude(t : Type, readHack : Boolean = false, target : String = "this.type") : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"""
                Annotation t = (Annotation)$target;"""
      case "string" ⇒
        if (readHack) """
                StringPool t = (StringPool) owner.Owner.Strings();"""
        else """
                StringType t = (StringType) this.type;"""

      case _ ⇒ ""
    }

    case t : ConstantLengthArrayType ⇒ s"""
                ConstantLengthArray<${mapType(t.getBaseType, true)}> type = (ConstantLengthArray<${mapType(t.getBaseType, true)}>) this.type.cast<${mapType(t.getBaseType, true)}, System.Object>();
                int size = type.length;${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : VariableLengthArrayType ⇒ s"""
                VariableLengthArray<${mapType(t.getBaseType, true)}> type = (VariableLengthArray<${mapType(t.getBaseType, true)}>) this.type.cast<${mapType(t.getBaseType, true)}, System.Object>();${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : ListType ⇒ s"""
                ListType<${mapType(t.getBaseType, true)}> type = (ListType<${mapType(t.getBaseType, true)}>) this.type.cast<${mapType(t.getBaseType, true)}, System.Object>();${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : SetType ⇒ s"""
                SetType<${mapType(t.getBaseType, true)}> type = (SetType<${mapType(t.getBaseType, true)}>) this.type.cast<${mapType(t.getBaseType, true)}, System.Object>();${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : MapType ⇒
      locally {
        val mt = s"MapType<${mapType(t.getBaseTypes.get(0), true)}, ${t.getBaseTypes.asScala.tail.map(mapType(_, true)).reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")}>"
        val gt = s"${mapType(t.getBaseTypes.get(0), true)}, ${t.getBaseTypes.asScala.tail.map(mapType(_, true)).reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")}"
        s"""
                $mt type = ($mt) this.type.cast<$gt>();
                de.ust.skill.common.csharp.api.FieldType keyType = type.keyType;
                de.ust.skill.common.csharp.api.FieldType valueType = type.valueType;"""
      }

    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""
        ${access(t.getSuperType)} t;
                if((de.ust.skill.common.csharp.@internal.FieldType)$target is ${access(t.getSuperType)})
                    t = (${access(t.getSuperType)})(object)($target);
                else
                    t = (${access(t.getSuperType)})((IInterfacePool)$target).getSuperPool();"""

    case t : InterfaceType ⇒ s"""
                Annotation t;
                // TODO we have to replace Annotation by the respective unrooted pool upon field creation to get rid of this distinction
                if ((de.ust.skill.common.csharp.@internal.FieldType)$target is Annotation)
                    t = (Annotation) (de.ust.skill.common.csharp.@internal.FieldType) $target;
                else
                    t = ((IUnrootedInterfacePool) $target).Type;"""

    case t : UserType if readHack ⇒ s"""
                ${access(t)} t = ((${access(t)})(object)$target);"""
    case _ ⇒ ""
  }

  /**
   * creates code to read all field elements
   */
  private final def readCode(t : UserType, f : Field) : String = {
    val declareD = s"${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner${
      if (null == t.getSuperType) ""
      else ".basePool"
    }).Data;"
    val fieldAccess = (
      if (null == t.getSuperType) "d[i]"
      else s"((${mapType(t)})d[i])") + s".${name(f)}"

    val pre = prelude(f.getType, true)

    val code = readCodeInner(f.getType)

    s"""$declareD$pre
                for (; i != h; i++) {${
      f.getType match {
        case t : ConstantLengthArrayType ⇒ s"""
            int s = size;
            ${mapType(f.getType)} v = new ArrayList(size);
            while (s-- > 0) {
                v.Add($code);
            }
            $fieldAccess = v;"""
        case t : VariableLengthArrayType ⇒ s"""
            int size = @in.v32();
            ${mapType(f.getType)} v = new ArrayList(size);
            while (size-- > 0) {
                v.Add($code);
            }
            $fieldAccess = v;"""
        case t : ListType ⇒ s"""
            int size = @in.v32();
            ${mapType(f.getType)} v = new List<${mapType(t.getBaseType())}>();
            while (size-- > 0) {
                v.Add($code);
            }
            $fieldAccess = v;"""
        case t : SetType ⇒ s"""
            int size = @in.v32();
            ${mapType(f.getType)} v = new HashSet<${mapType(t.getBaseType())}>();
            while (size-- > 0) {
                v.Add($code);
            }
            $fieldAccess = v;"""
        case _ ⇒ s"""
            $fieldAccess = $code;"""
      }
    }
                }
"""
  }

  private final def readCodeInner(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"(${mapType(t)})t.readSingleField(@in)"
      case "string"     ⇒ "t.get(@in.v32())"
      case "bool"       ⇒ "@in.@bool()"
      case _            ⇒ s"""@in.${t.getSkillName}()"""
    }
    case t : SingleBaseTypeContainer ⇒ readCodeInner(t.getBaseType)

    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"(${mapType(t)})t.getByID(@in.v32())"
    case t : InterfaceType ⇒ s"(${mapType(t)})t.readSingleField(@in)"

    case t : UserType ⇒ s"(${mapType(t, false)})t.getByID(@in.v32())"
    case _ ⇒ s"castMap<${mapType(t.asInstanceOf[MapType].getBaseTypes().get(0))}, ${transformMapTypes(t.asInstanceOf[MapType].getBaseTypes(), 1)}, System.Object, System.Object>((Dictionary<System.Object, System.Object>)((de.ust.skill.common.csharp.@internal.FieldType)this.type).readSingleField(@in))"
  }

  def transformMapTypes(baseTypes: java.util.List[Type], count: Integer) : String = if((baseTypes.size() - 1) == count) s"${mapType(baseTypes.get(count))}" else s"System.Collections.Generic.Dictionary<${mapType(baseTypes.get(count))}, ${transformMapTypes(baseTypes, count + 1)}>"


  /**
   * generates offset calculation code and a prelude with fields used in the
   * inner loop
   *
   * @return (prelude, code, isFast)
   */
  private final def offsetCode(t : UserType, f : Field, fieldType : Type, fieldActualType : String) : (String, Boolean) = {

    val fieldAccess = (
      if (null == t.getSuperType) "d[i]"
      else s"((${mapType(t)})d[i])") + s".${name(f)}"

    val code = fieldType match {

      case fieldType : GroundType ⇒ fieldType.getSkillName match {

        case "annotation" ⇒ s"""${mapType(f.getType)} v = $fieldAccess;
                    if(null==v)
                        result += 2;
                    else
                        result += t.singleOffset(v);"""

        case "string" ⇒ s"""string v = $fieldAccess;
                    if(null==v)
                        result++;
                    else
                        result += t.singleOffset(v);"""

        case "i8" | "bool" ⇒ return fastOffsetCode(0);

        case "i16"         ⇒ return fastOffsetCode(1);

        case "i32" | "f32" ⇒ return fastOffsetCode(2);

        case "i64" | "f64" ⇒ return fastOffsetCode(3);

        case "v64"         ⇒ s"""result += V64.singleV64Offset($fieldAccess);"""
        case _ ⇒ s"""
        throw new NoSuchMethodError();"""
      }

      case fieldType : ConstantLengthArrayType ⇒ s"""${mapType(f.getType)} v = null == $fieldAccess ? null : v = (${mapType(f.getType)})$fieldAccess;
                    if (v.Count != type.length)
                        throw new Exception("constant length array has wrong size");

                    ${offsetCodeInner(fieldType.getBaseType, "v")}"""

      case fieldType : SingleBaseTypeContainer ⇒ s"""${mapType(f.getType)} v = null == $fieldAccess ? null : ${if (fieldType.isInstanceOf[ListType]) s"((${mapType(fieldType)})$fieldAccess).Cast<${mapType(f.getType().asInstanceOf[ListType].getBaseType)}>().ToList();"
                  else if (fieldType.isInstanceOf[VariableLengthArrayType]) s"(${mapType(f.getType)})$fieldAccess;"
                  else if (fieldType.isInstanceOf[SetType]) s"new ${mapType(f.getType)}(((${mapType(fieldType)})$fieldAccess).Cast<${mapType(f.getType().asInstanceOf[SetType].getBaseType)}>());"
                  }

                    int size = null == v ? 0 : v.Count;
                    if (0 == size)
                        result++;
                    else {
                        result += V64.singleV64Offset(size);
                        ${offsetCodeInner(fieldType.getBaseType, "v")}
                    }"""

      case fieldType : MapType ⇒ s"""${mapType(f.getType)} v = castMap<${mapType(f.getType().asInstanceOf[MapType].getBaseTypes().get(0))}, ${transformMapTypes(f.getType().asInstanceOf[MapType].getBaseTypes(), 1)}, ${mapType(fieldType.getBaseTypes().get(0))}, ${transformMapTypes(fieldType.getBaseTypes(), 1)}>($fieldAccess);
                    if(null==v || v.Count == 0)
                        result++;
                    else {

                        ${mapType(f.getType().asInstanceOf[MapType].getBaseTypes().get(0))}[] keysArray = new ${mapType(f.getType().asInstanceOf[MapType].getBaseTypes().get(0))}[v.Keys.Count];
                        v.Keys.CopyTo(keysArray, 0);
                        ICollection keysList = new List<object>();
                        foreach (object key in keysArray)
                        {
                            ((List<object>)keysList).Add(key);
                        }

                        ${transformMapTypes(f.getType().asInstanceOf[MapType].getBaseTypes(), 1)}[] valuesArray = new ${transformMapTypes(f.getType().asInstanceOf[MapType].getBaseTypes(), 1)}[v.Values.Count];
                        v.Values.CopyTo(valuesArray, 0);
                        ICollection valuesList = new List<object>();
                        foreach (object value in valuesArray)
                        {
                            ((List<object>)valuesList).Add(value);
                        }

                        result += V64.singleV64Offset(v.Count);
                        result += ((de.ust.skill.common.csharp.@internal.FieldType)keyType).calculateOffset(keysList);
                        result += ((de.ust.skill.common.csharp.@internal.FieldType)valueType).calculateOffset(valuesList);
                    }"""

      case fieldType : UserType ⇒ s"""${mapType(f.getType)} instance = $fieldAccess;
                    if (null == instance) {
                        result += 1;
                        continue;
                    }
                    result += V64.singleV64Offset(instance.SkillID);"""

      case fieldType : InterfaceType if fieldType.getSuperType.getSkillName != "annotation" ⇒
                s"""${mapType(fieldType)} instance = $fieldAccess;
                    if (null == instance) {
                        result += 1;
                        continue;
                    }
                    result += V64.singleV64Offset(((SkillObject) instance).SkillID);"""

      case fieldType : InterfaceType ⇒        s"""${mapType(fieldType)} v = $fieldAccess;

                    if(null==v)
                        result += 2;
                    else
                        result += t.singleOffset((SkillObject)v);"""

      case _ ⇒      s"""throw new NoSuchMethodError();"""
    }

    (code, false)
  }

  private final def offsetCodeInner(t : Type, target : String) : String = t match {
    case fieldType : GroundType ⇒ fieldType.getSkillName match {
      case "i8" | "bool" ⇒ return "result += size;";

      case "i16"         ⇒ return "result += (size<<1);";

      case "i32" | "f32" ⇒ return "result += (size<<2);";

      case "i64" | "f64" ⇒ return "result += (size<<3);";

      case "v64" ⇒ s"""foreach(long x in $target)
                    result += V64.singleV64Offset(x);"""

      case _ ⇒ s"""result += this.type.groundType.calculateOffset($target);"""
    }
    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""foreach(${mapType(t.getSuperType)} x in $target)
                    result += null==x?1:V64.singleV64Offset(x.SkillID);"""

    case t : UserType ⇒ s"""foreach(${mapType(t)} x in $target)
                    result += null==x?1:V64.singleV64Offset(x.SkillID);"""

    case _ ⇒ s"""result += this.type.groundType.calculateOffset($target);"""
  }

  private final def fastOffsetCode(shift : Int) =
    (
      if (shift != 0) s"offset += (h-i) << $shift;"
      else "offset += (h-i);",
      true)

  /**
   * creates code to write exactly one field element
   */
  private final def writeCode(t : UserType, f : Field) : String = {
    val declareD = s"${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner${
      if (null == t.getSuperType) ""
      else ".basePool"
    }).Data;"
    val fieldAccess = (
      if (null == t.getSuperType) "d[i]"
      else s"((${mapType(t)})d[i])") + s".${name(f)}"

    val pre = prelude(f.getType)

    val code = writeCode(f.getType, fieldAccess)

    s"""$declareD$pre
                for (; i != h; i++) {
                    $code;
                }
"""
  }

  private final def writeCode(t : Type, fieldAccess : String) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"t.writeSingleField($fieldAccess, @out)"
      case "string"     ⇒ s"t.writeSingleField($fieldAccess, @out)"
      case "bool"       ⇒ s"""@out.@bool($fieldAccess)"""
      case _            ⇒ s"""@out.${t.getSkillName}($fieldAccess)"""
    }

    case t : ConstantLengthArrayType ⇒ s"""
        ${mapType(t)} x = $fieldAccess;
        foreach (${mapType(t.getBaseType)} e in x){
            ${writeCode(t.getBaseType, "e")};
        }"""

    case t : SingleBaseTypeContainer ⇒ s"""
        ${mapType(t)} x = $fieldAccess;
        int size = null == x ? 0 : x.Count;
        if (0 == size) {
            @out.i8((sbyte) 0);
        } else {
            @out.v64(size);
            foreach (${mapType(t.getBaseType)} e in x){
                ${writeCode(t.getBaseType, "e")};
            }
        }"""

    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""SkillObject v = (SkillObject)$fieldAccess;
            if(null == v)
                @out.i8((sbyte)0);
            else
                @out.v64(v.SkillID)"""

    case t : InterfaceType ⇒ s"t.writeSingleField((SkillObject)$fieldAccess, @out)"

    case t : UserType ⇒ s"""${mapType(t)} v = $fieldAccess;
            if(null == v)
                @out.i8((sbyte)0);
            else
                @out.v64(v.SkillID)"""
    case _ ⇒ s"((de.ust.skill.common.csharp.@internal.FieldType)this.type).writeSingleField($fieldAccess, @out)"
  }
}
