/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Field
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.MonotoneRestriction

/**
 * creates header and implementation for all type definitions
 * 
 * @author Timm Felden
 */
trait TypesMaker extends GeneralOutputMaker {

  @inline private final def fieldName(implicit f : Field) : String = escaped(f.getName.capital())
  @inline private final def localFieldName(implicit f : Field) : String = internalName(f)

  abstract override def make {
    super.make

    makeHeader
    makeSource
  }

  private final def makeHeader {

    // one header per base type
    for(base <- IR.par if null == base.getSuperType) {
      val out = files.open(s"TypesOf${name(base)}.h")

      //includes package
      out.write(s"""${beginGuard(s"types_of_${name(base)}")}
#include <skill/api/types.h>
#include <skill/api/SkillException.h>
#include <cassert>
#include <vector>
#include <set>
#include <map>

namespace skill{
    namespace internal {
        template<class T>
        class Book;

        template<class T, class B>
        class StoragePool;
    }
}

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
${
  if(createVisitors) s"""
    // predef visitor
    namespace api {
        class ${name(base)}Visitor;
    }
"""
  else ""
}
    // type predef for cyclic dependencies${
  (for (t ← IR) yield s"""
    class ${name(t)};""").mkString
}
    // type predef known fields for friend declarations
    namespace internal {${
  (for (t ← IR if base == t.getBaseType; f <- t.getFields) yield s"""
        class ${knownField(f)};""").mkString
}
    }
    // begin actual type defs
""")


    for (t ← IR if base == t.getBaseType){
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
        friend class ::skill::internal::StoragePool<${name(t)},${name(t.getBaseType)}>;${
  (for (f <- t.getFields) yield s"""
        friend class internal::${knownField(f)};""").mkString
}

    protected:
""")
      // fields
	    out.write((for(f <- t.getFields if !f.isConstant)
        yield s"""    ${mapType(f.getType())} ${localFieldName(f)};
""").mkString)

      // constructor
    	out.write(s"""
        $Name() { }

        $Name(::skill::SKilLID _skillID${
    	  (for(f <- t.getAllFields if !f.isConstant()) yield s""",
    	    ${mapType(f.getType)} __${name(f)} = ${defaultValue(f)}""").mkString
    	}) {
            this->id = _skillID;${
    	  (for(f <- t.getAllFields if !f.isConstant()) yield s"""
            this->${localFieldName(f)} = __${name(f)};""").mkString
    	}
        }

    public:
""")

      // accept visitor
      if(createVisitors){
        out.write(s"""
        virtual void accept(api::${name(base)}Visitor *v);
""")
      }

	    // reveal skill id
      if(revealSkillID && null==t.getSuperType)
        out.write("""
        inline ::skill::SKilLID skillID() const { return this->id; }
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
          s"${
          f.getRestrictions.map {
            //@range
            case r:IntRangeRestriction ⇒
              (r.getLow == Long.MinValue, r.getHigh == Long.MaxValue) match {
              case (true, true)   ⇒ ""
              case (true, false)  ⇒ s"assert(${name(f)} <= ${r.getHigh}L);"
              case (false, true)  ⇒ s"assert(${r.getLow}L <= ${name(f)});"
              case (false, false) ⇒ s"assert(${r.getLow}L <= ${name(f)} && ${name(f)} <= ${r.getHigh}L);"
            }
            case r:FloatRangeRestriction if("f32".equals(f.getType.getName)) ⇒
              s"assert(${r.getLowFloat}f <= ${name(f)} && ${name(f)} <= ${r.getHighFloat}f);"
            case r:FloatRangeRestriction ⇒
              s"assert(${r.getLowDouble} <= ${name(f)} && ${name(f)} <= ${r.getHighDouble});"

            //@monotone modification check
            case r:MonotoneRestriction ⇒ "assert(id == -1L); "

            case _ ⇒ ""
          }.mkString
          }$localFieldName = ${name(f)};"
      }

      if(f.isConstant)
        out.write(s"""
        ${comment(f)}inline ${mapType(f.getType)} get$fieldName() const {$makeGetterImplementation}
""")
      else
        out.write(s"""
        ${comment(f)}inline ${mapType(f.getType)} get$fieldName() const {$makeGetterImplementation}
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

        virtual std::string toString() const { return std::string(typeName) + std::to_string(this->id); }

        virtual void prettyString(std::ostream &os) const {
            os << "${t.getName.capital}#" << id;
        }
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
      
      // create visitor implementation
      if(createVisitors)
        out.write(s"""
    // visitor implementation
    namespace api {
        struct ${name(base)}Visitor {${
          (for(t <- IR if t.getBaseType == base) yield s"""
            virtual void visit(${name(t)} *node) {};""").mkString
        }
        };
    }
""")

      // close name spaces
      out.write(s"""${packageParts.map(_ ⇒ "}").mkString}
$endGuard""")

      out.close()
    }
  }

  private final def makeSource {

    // one file per base type
    for(base <- IR if null == base.getSuperType) {
      val out = files.open(s"TypesOf${name(base)}.cpp")
      out.write(s"""#include "TypesOf${name(base)}.h"
#include <skill/internal/AbstractStoragePool.h>${
      (for(t <- IR if base == t.getBaseType) yield s"""

const char *const $packageName::${name(t)}::typeName = "${t.getSkillName}";
const char *$packageName::${name(t)}_UnknownSubType::skillName() const {
    return owner->name->c_str();
}${
  if(createVisitors) s"""
void $packageName::${name(t)}::accept($packageName::api::${name(t.getBaseType)}Visitor *v) {
    v->visit(this);
}"""
  else ""
}""").mkString
    }
""")
      out.close()
    }
  }
}
