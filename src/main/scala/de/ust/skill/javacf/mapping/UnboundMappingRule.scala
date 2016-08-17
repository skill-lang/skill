package de.ust.skill.javacf.mapping

import scala.collection.JavaConversions._
import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.typing.TypeRule

class UnboundMappingRule(javaType: String, fieldNames: List[String], total: Boolean) extends MappingRule {

  override def toString(): String = if (total)
    s"new! $javaType;" else s"new $javaType {\n${fieldNames.mkString(";\n")}\n}"

  override def getJavaTypeName(): String = javaType;

  def bind(skill: TypeContext, java: TypeContext): List[TypeRule] = {
    java.getUsertypes.foreach { ut =>
      {
        ut.getFields.foreach { f =>
          if (!java.types.containsKey(f.getType.getName.getFqdn))
            throw new RuntimeException(s"Field ${f.getName.getSkillName} in type ${ut.getName.getFqdn} was mapped but the respective type (${f.getType.getName.getFqdn}) does not exist.")
        }
      }
    }
    List()
  }
}