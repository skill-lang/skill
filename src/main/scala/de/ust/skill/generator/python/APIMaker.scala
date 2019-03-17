/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

trait APIMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"api.py")
    var path = files.getOutPath

    //package & imports
    out.write(
      s"""
from python.src.${packagePrefix()}internal import SkillState, SkillObject, NamedType, Mode
""")
    for(t <- IR) {
      val customizations = t.getCustomizations.filter(_.language.equals("python")).toArray

      // no package
      out.write(s"""
${customizations.flatMap(_.getOptions.get("import")).map(i⇒s"import $i\n").mkString}""")//TODO

      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      out.write(s"""
class ${name(t)}(${
        if (null != t.getSuperType) { name(t.getSuperType) }
        else { "SkillObject" }
      }):\n${comment(t)}
    def __init__(self, skillID=-1${appendConstructorArguments(t)}):
        \"\"\"
        Create a new unmanaged ${t.getName.capital()}. Allocation of objects without using the
        access factory method is discouraged.
        :param: Used for internal construction only!
        \"\"\"
        super(${name(t)}, self).__init__(skillID)
        self.skillName = "${t.getSkillName}"
        ${relevantFields.map { f ⇒ s"self.${name(f)}: ${mapType(f.getType)} = ${name(f)}" }.mkString("\n        ")}
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
            ${name(f)} = ${defaultValue(f)}
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

class SubType${name(t)}(${name(t)}, NamedType):
    \"\"\"
    Generic sub types of this type.
    \"\"\"

    def __init__(self, tPool, skillID=-1):
        \"\"\"internal use only!!!\"\"\"
        super(SubType${name(t)}, self).__init__(skillID)
        self.tPool = tPool

    def skillName(self):
        return self.tPool.name

    def toString(self):
        return self.skillName() + "#" + self.skillID
""")
    }
      out.write(
          s"""

class SkillFile(SkillState):
    \"\"\"
    An abstract skill file that is hiding all the dirty implementation details
    from you.
    \"\"\"
""")
      var knownTypes = "["
      for(t <- IR){
          if(t == IR.head){knownTypes = knownTypes + s"""${name(t)}"""}
          else{knownTypes = knownTypes + s""", ${name(t)}"""}
      }
      knownTypes = knownTypes + "]"

      var knownSubTypes = """["""
      for (t <- IR) {
          if (t == IR.head) {knownSubTypes= knownSubTypes + s"""SubType${name(t)}"""}
          else {knownSubTypes = knownSubTypes + s""", SubType${name(t)}"""}
      }
      knownSubTypes = knownSubTypes + """]"""
      out.write(s"""
    @staticmethod
    def open(path, *mode):
        \"\"\"
        Create a new skill file based on argument path and mode.
        \"\"\"
        return SkillState.open(path, mode, $knownTypes, $knownSubTypes)
""")
    out.close()
  }
}
