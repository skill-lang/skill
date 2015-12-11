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

  @inline private final def fieldName(implicit f : Field) : String = escaped(f.getName.capital())
  @inline private final def localFieldName(implicit f : Field) : String = escaped("_" + f.getName.camel())

  abstract override def make {
    super.make
    
    makeHeader
    makeSource
  }
  
  private final def makeHeader {

    val out = open(s"Types.h")

      //includes package
    out.write(s"""${beginGuard("types")}
#include <skill/api/Object.h>
#include <skill/api/SkillException.h>
#include <cassert>
#include <vector>
#include <set>
#include <map>

namespace skill{
    namespace internal {
        template<class T>
        class Book;
    }
}

${packageParts.mkString("namespace ", " {\nnamespace", " {")}

    // type predef for cyclic dependencies${
  (for (t ← IR) yield s"""
    class ${name(t)};""").mkString
}
    // begin actual type defs
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
        friend class ::skill::internal::Book<${name(t)}>;
    protected:
""")
      // fields
	    out.write((for(f <- t.getFields if !f.isConstant)
        yield s"""    ${mapType(f.getType())} ${localFieldName(f)};
""").mkString)

      // constructor
    	out.write(s"""
        $Name() { }

    public:

        $Name(::skill::SKilLID _skillID) {
            this->id = _skillID;
        }
""")

	  // reveal skill id
      if(revealSkillID && null==t.getSuperType)
        out.write("""
    inline ::skill:SKilLID skillID() { return this->id; }
""")

  //${if(revealSkillID)"" else s"protected[${packageName}] "}final def getSkillID = skillID

	///////////////////////
	// getters & setters //
	///////////////////////
	    for(f <- t.getFields) {
        implicit val thisF = f;

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw ::skill::SkillException::IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint");"""
        else if(f.isConstant)
          s"return (${mapType(f.getType)})0x${f.constantValue().toHexString};"
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
                f.getRestrictions.collect{case r:IntRangeRestriction⇒r}.map{r ⇒ s"""assert(${r.getLow}L <= ${name(f)} && ${name(f)} <= ${r.getHigh}L); """}.mkString("")
              else if("f32".equals(f.getType.getName))
                f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""assert(${r.getLowFloat}f <= ${name(f)} && ${name(f)} <= ${r.getHighFloat}f); """}.mkString("")
              else if("f64".equals(f.getType.getName))
               f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""assert(${r.getLowDouble} <= ${name(f)} && ${name(f)} <= ${r.getHighDouble}); """}.mkString("")
              else
                ""
            }
            else
              ""
          }${//@monotone modification check
            if(!t.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty){
              s"""assert(id == -1L); """
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

        static const char *const typeName;

        virtual const char *skillName() const { return typeName; }

        virtual std::string toString() { return std::string(typeName) + std::to_string(this->id); }
    };

    class ${name(t)}_UnknownSubType : public ${name(t)} {
        const ::skill::internal::AbstractStoragePool *owner;

        //! bulk allocation constructor
        ${name(t)}_UnknownSubType() { };

        friend class ::skill::internal::Book<${name(t)}_UnknownSubType>;

        //final override def prettyString : String = s"$$getTypeName#$$skillID"

    public:
        /**
         * !internal use only!
         */
        inline void byPassConstruction(::skill::SKilLID id, const ::skill::internal::AbstractStoragePool *owner) {
            this->id = id;
            this->owner = owner;
        }

        ${name(t)}_UnknownSubType(::skill::SKilLID id) : owner(nullptr) {
            throw ::skill::SkillException("one cannot create an unknown object without supllying a name");
        }

        virtual const char *skillName() const;
    };
""");
    }

    // close name spaces
    out.write(s"""${packageParts.map(_ ⇒ "}").mkString}
$endGuard""")

    out.close()
  }

  private final def makeSource {
    val out = open(s"Types.cpp")
    out.write(s"""#include "Types.h"
#include <skill/internal/AbstractStoragePool.h>${
(for(t <- IR) yield s"""
const char *const $packageName::${name(t)}::typeName = "${t.getSkillName}";
const char *$packageName::${name(t)}_UnknownSubType::skillName() const {
    return owner->name->c_str();
}
""").mkString
    }
""")
    out.close()
  }
}
