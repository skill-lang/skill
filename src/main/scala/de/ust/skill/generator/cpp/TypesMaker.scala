/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import java.io.PrintWriter

import scala.collection.JavaConversions._

import de.ust.skill.ir._
import de.ust.skill.ir.restriction._

/**
 * creates header and implementation for all type definitions
 * 
 * @author Timm Felden
 */
trait TypesMaker extends GeneralOutputMaker {

  @inline def fieldName(implicit f : Field) : String = escaped(f.getName.capital())
  @inline def localFieldName(implicit f : Field) : String = escaped("_" + f.getName.camel())

  abstract override def make {
    super.make

    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1)
    else this.packageName;

    val out = open(s"Types.h")


      //includes package
    out.write(s"""
// include???

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
""")


    for (t ← IR){
      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)
      val Name = name(t)
      val SuperName = if (null != t.getSuperType()) name(t.getSuperType)
        else "::skill::api::Object"

      //class declaration
      out.write(s"""
${
        comment(t)
}class $Name : public $SuperName {
protected:
""")
      // fields
	    out.write((for(f <- t.getFields if !f.isInstanceOf[View] && !f.isConstant)
        yield s"""${mapType(f.getType())} ${localFieldName(f)};
""").mkString)

      // constructor
    	out.write(s"""
  $Name(::skill::SkillID _skillID) {
    this->id = _skillID;
  }

public:
""")

	  // reveal skill id
      if(revealSkillID && null==t.getSuperType)
        out.write("""
    inline ::skill:SkillID skillID() { return this->id; }
""")

  //${if(revealSkillID)"" else s"protected[${packageName}] "}final def getSkillID = skillID

	///////////////////////
	// getters & setters //
	///////////////////////
	    for(f <- t.getFields if !f.isInstanceOf[View]) {
        implicit val thisF = f;

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw ::skill::SkillException::IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint");"""
        else if(f.isConstant)
          s"return ${f.constantValue().toString}.to${mapType(f.getType)};"
        else
          s"return $localFieldName;"
      }

      def makeSetterImplementation:String = {
        if(f.isIgnored)
          s"""throw ::skill::SkillException::IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint");"""
        else
          s"${ //@range check
            if(f.getType().isInstanceOf[GroundType]){
              if(f.getType().asInstanceOf[GroundType].isInteger)
                f.getRestrictions.collect{case r:IntRangeRestriction⇒r}.map{r ⇒ s"""assert(${r.getLow}L <= ${name(f)} && ${name(f)} <= ${r.getHigh}L, "${name(f)} has to be in range [${r.getLow};${r.getHigh}]"); """}.mkString("")
              else if("f32".equals(f.getType.getName))
                f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""assert(${r.getLowFloat}f <= ${name(f)} && ${name(f)} <= ${r.getHighFloat}f, "${name(f)} has to be in range [${r.getLowFloat};${r.getHighFloat}]"); """}.mkString("")
              else if("f64".equals(f.getType.getName))
               f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""assert(${r.getLowDouble} <= ${name(f)} && ${name(f)} <= ${r.getHighDouble}, "${name(f)} has to be in range [${r.getLowDouble};${r.getHighDouble}]"); """}.mkString("")
              else
                ""
            }
            else
              ""
          }${//@monotone modification check
            if(!t.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty){
              s"""assert(skillID == -1L, "${t.getName} is specified to be monotone and this instance has already been subject to serialization!"); """
            }
            else
              ""
        }$localFieldName = ${name(f)};"
      }

      if(f.isConstant)
        out.write(s"""
  ${comment(f)}inline ${mapType(f.getType)} get$fieldName() {$makeGetterImplementation}
""")
      else
        out.write(s"""
  ${comment(f)}inline ${mapType(f.getType)} get$fieldName() {$makeGetterImplementation}
  ${comment(f)}inline void set$fieldName(${mapType(f.getType)} ${name(f)}) {$makeSetterImplementation}
""")
    }

    out.write(s"""
/*  override def prettyString : String = s"${name(t)}(#$$skillID${
    (
        for(f <- t.getAllFields)
          yield if(f.isIgnored) s""", ${f.getName()}: <<ignored>>"""
          else if (!f.isConstant) s""", ${if(f.isAuto)"auto "else""}${f.getName()}: $${${name(f)}}"""
          else s""", const ${f.getName()}: ${f.constantValue()}"""
    ).mkString
  })"*/

private:
  static const std::string typeName = std::string("${t.getSkillName}");
public:
  virtual const std::string* getTypeName() { return &typeName; }

  virtual std::string toString() { return typeName + std::to_string(this->id); }
};
""")

      out.write(s"""
namespace ${name(t)} {
  class UnknownSubType {

    //final override def prettyString : String = s"$$getTypeName#$$skillID"
  };
}
""");
    }

    // close name spaces
    out.write(s"${packageParts.map(_ ⇒ "}").mkString}")

    out.close()
  }
}
