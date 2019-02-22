/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

trait TypesMaker extends GeneralOutputMaker {
    abstract override def make {
    super.make

    for(t <- IR) {
      val out = files.open(s"${name(t)}.py")

      val customizations = t.getCustomizations.filter(_.language.equals("python")).toArray

      // no package
      out.write(s"""

from src.internal.FieldDeclaration import NamedType
from src.internal.SkillObject import SkillObject
from src.internal.StoragePool import StoragePool
${customizations.flatMap(_.getOptions.get("import")).map(i⇒s"import $i\n").mkString}
""")//TODO


      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      out.write(s"""
class ${name(t)}${
        if (null != t.getSuperType()) { name(t.getSuperType) }
        else { "(SkillObject)" }
      }:\n${comment(t)}
    def __init__(self${appendConstructorArguments(t)}, skillID=-1):
        \"\"\"
        Create a new unmanaged ${t.getName.capital()}. Allocation of objects without using the
        access factory method is discouraged.
        :param: Used for internal construction only!
        \"\"\"
        super(${name(t)}, self).__init__(skillID)
        self.skillName = ${t.getSkillName}
        ${relevantFields.map { f ⇒ s"self.${name(f)} = ${name(f)}" }.mkString("\n        ")}
""")

      var implementedFields = t.getFields.toSeq

      ///////////////////////
      // getters & setters //
      ///////////////////////
      for(f <- implementedFields) {
          if(f.isIgnored){
            ""
          } else {
            s"""
            ${
              if(f.isAuto()) "transient " //TODO
              else ""
            }${name(f)} = ${defaultValue(f)}
"""
          }
      }

      // custom fields
      for(c <- customizations) {
        out.write(s"""
    ${comment(c)} ${name(c)}
""")
      }

      out.write(s"""
    class SubType(${name(t)}, NamedType):
        \"\"\"
        Generic sub types of this type.
        \"\"\"

        def __init__(self, tPool, skillID):
            \"\"\"internal use only!!!\"\"\"
            super(SubType, self).__init__(skillID)
            super(skillID)
            self.tPool = tPool

        def skillName(self):
            return self.tPool.name

        def toString(self):
            return self.skillName + "#" + self.skillID
""")
      out.close()
    }
  }
}
