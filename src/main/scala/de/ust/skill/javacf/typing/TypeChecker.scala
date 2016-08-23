package de.ust.skill.javacf.typing

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import javassist.CtClass
import javassist.Modifier
import javassist.NotFoundException

class TypeChecker {

  def check(rules: List[TypeRule], from: TypeContext, to: TypeContext, reflectionMap: HashMap[Type, CtClass]) {
    val fromTypes: Set[Type] = collection.mutable.Set() ++ from.getUsertypes.asScala
    val toTypes: Set[Type] = collection.mutable.Set() ++ to.getUsertypes.asScala
    val checked = HashMap.empty[Type, Type]
    var failed: Boolean = false

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
      _ match {
        case equation: TypeEquation ⇒ {
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
        }
        case targetExists: TargetTypeExists ⇒ {
          toTypes -= targetExists.getTargetType
          if (to.get(targetExists.getTargetType.getName.getFqdn) == null) {
            failed = true;
            println(s"${targetExists.getTargetType.getName} not found but must exist!")
          }
        }
        case fieldAccessible: FieldAccessible ⇒ {
          val field = fieldAccessible.getField
          val typ = fieldAccessible.getType
          val reflection = reflectionMap.get(typ).get
          val fieldReflection = reflection.getField(field.getName.getSkillName)

          val hasGetter = try {
            reflection.getDeclaredMethod(s"get${field.getName.capital()}")
            true
          } catch {
            case e: NotFoundException ⇒ false
          }
          if (!hasGetter && ((fieldReflection.getModifiers & Modifier.PUBLIC) != Modifier.PUBLIC)) {
            failed = true
            println(s"Need either Method 'public ${field.getType.getSkillName} get${field.getName.capital()}()' or field ${field.getName.getSkillName} must be public in type ${typ.getName.getSkillName}.");
          }

        }
      }
    }
    fromTypes.foreach { unmapped => println(s"warning: SKilL type $unmapped has not been mapped to any Java type") }
    toTypes.foreach { unmapped => println(s"warning: Java type $unmapped is not used by any mapping") }
    if (failed) {
      throw new RuntimeException(s"There were type errors in the mapping")
    }
  }

}