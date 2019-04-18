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
from python.src.${packagePrefix()}internal import SkillState, SkillObject, Mode
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
      out.write(s"""
    def get${escaped(f.getName.capital)}(self):
        return self.${name(f)}

    def set${escaped(f.getName.capital)}(self, value):
        ${if (f.isConstant){
        s"""\"\"\" Constant field. You cannot set ${name(f)} \"\"\"
        raise Exception("You are not allowed to set ${name(f)}")
        """
        } else {
          s"""assert isinstance(value, ${mapType(f.getType)}) or value is None
        self.${name(f)} = value""".stripMargin
        }
      }
""")
      }
      // custom fields
      for(c <- customizations) {
        out.write(s"""
    ${comment(c)} ${name(c)}
""")
      }
    }
      out.write(
          s"""

class SkillFile:
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

      out.write(s"""
    @staticmethod
    def open(path, *mode):
        \"\"\"
        Create a new skill file based on argument path and mode.
        \"\"\"
        return SkillState.open(path, mode, $knownTypes)
""")
    out.close()
  }
}
