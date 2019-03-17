/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.typing

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

import de.ust.skill.ir.Field
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.jforeign.ReflectionContext
import javassist.Modifier
import javassist.NotFoundException
import de.ust.skill.ir.UserType

class TypeChecker {

  def check(rules : List[TypeRule], from : TypeContext, to : TypeContext, rc : ReflectionContext) : (Set[Type], Set[Field]) = {
    val fromTypes : Set[Type] = collection.mutable.Set() ++ from.getUsertypes.asScala
    val toTypes : Set[Type] = collection.mutable.Set() ++ to.getUsertypes.asScala
    val checked = HashMap.empty[Type, Type]
    var failed : Boolean = false

    val mappedFields : Set[Field] = Set()
    val mappedTypes : Set[Type] = Set()

    checked += (from.get("bool") → to.get("bool"))
    checked += (from.get("i8") → to.get("i8"))
    checked += (from.get("i16") → to.get("i16"))
    checked += (from.get("i32") → to.get("i32"))
    checked += (from.get("i64") → to.get("i64"))
    checked += (from.get("v64") → to.get("i64"))
    checked += (from.get("f32") → to.get("f32"))
    checked += (from.get("f64") → to.get("f64"))
    checked += (from.get("string") → to.get("string"))

    rules.foreach {
      case equation : TypeEquation ⇒ {
        val left = equation.getLeft
        val right = equation.getRight

        fromTypes -= left
        toTypes -= right
        if (checked contains left) {
          if (checked(left) != right) {
            println(s"${left} is mapped to ${right} but it was mapped to ${checked(left)} before")
            failed = true
          }
        } else {
          checked += (left → right)
        }

        // check super relation
        left match {
          case left : UserType if (null == left.getSuperType) ⇒
          // there is no specified super type, hence no action is required
          // @note add check that super type is in fact unmapped?

          case left : UserType if (null != left.getSuperType) && (null == right.asInstanceOf[UserType].getSuperType) ⇒
            println(s"${left} is mapped to ${right} but ${left} has a super type")
            failed = true

          case left : UserType if null != right.asInstanceOf[UserType].getSuperType && (checked(left.getSuperType) != right.asInstanceOf[UserType].getSuperType) ⇒
            println(s"${left} is mapped to ${right} but their super types are not mapped to the same type")
            failed = true

          case _ ⇒ // all is well
        }
      }
      case targetExists : TargetTypeExists ⇒ {
        toTypes -= targetExists.getTargetType
        if (to.get(targetExists.getTargetType.getName.getFqdn) == null) {
          failed = true;
          println(s"${targetExists.getTargetType.getName} not found but must exist!")
        }
      }
      case fieldAccessible : FieldAccessible ⇒ {
        val field = fieldAccessible.getField
        val typ = fieldAccessible.getType
        val reflection = rc.map(typ)
        val fieldReflection = reflection.getField(field.getName.getSkillName)

        val hasGetter = try {
          reflection.getDeclaredMethod(s"get${field.getName.capital()}")
          true
        } catch {
          case e : NotFoundException ⇒ false
        }
        if (!hasGetter && ((fieldReflection.getModifiers & Modifier.PUBLIC) != Modifier.PUBLIC)) {
          failed = true
          println(s"""Need either Method 'public ${field.getType.getSkillName} get${field.getName.capital()}()' or
  field ${field.getName.getSkillName} must be public in type ${typ.getName.getSkillName}.""");
        }
      }
      case fmo : FieldMappedOnce ⇒ {
        if (mappedFields contains fmo.getField) {
          failed = true
          println(s"Field ${fmo.getField.getName.getSkillName} of type ${fmo.getDeclaringType.getName.getSkillName} participates in more than one mapping")
        } else mappedFields += fmo.getField
      }
      case tmo : TypeMappedOnce ⇒ {
        if (mappedTypes contains tmo.getType) {
          failed = true
          println(s"Type ${tmo.getType.getName.getSkillName} participates in more than one mapping")
        } else mappedTypes += tmo.getType
      }
    }
    fromTypes.foreach { unmapped ⇒ println(s"warning: SKilL type $unmapped has not been mapped to any Java type") }
    toTypes.foreach { unmapped ⇒ println(s"warning: Java type $unmapped is not used by any mapping") }
    if (failed) {
      throw new RuntimeException(s"There were type errors in the mapping")
    }
    (mappedTypes, mappedFields)
  }
}
