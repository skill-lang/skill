/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap

import de.ust.skill.ir.InterfaceType

trait TypesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for(t <- IR) {
      val out = files.open(s"${name(t)}.cs")
      
      val customizations = t.getCustomizations.filter(_.language.equals("csharp")).toArray

      // package
      out.write(s"""
using System;

${
  if(visited.contains(t.getSkillName)) s"""using ${this.packageName}.api;
"""
  else ""
}using NamedType = de.ust.skill.common.csharp.@internal.NamedType;
using SkillObject = de.ust.skill.common.csharp.@internal.SkillObject;
using AbstractStoragePool = de.ust.skill.common.csharp.@internal.AbstractStoragePool;
${customizations.flatMap(_.getOptions.get("import")).map(i⇒s"using $i;\n").mkString}

namespace ${this.packageName}
{
""")
    
    

      val packageName = 
        if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1) 
        else this.packageName;

      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      out.write(s"""
    ${
        comment(t)
    }${
        suppressWarnings
    }public class ${name(t)} : ${
          if (null != t.getSuperType()) { name(t.getSuperType) }
          else { "SkillObject" }
          }${
          if(t.getSuperInterfaces.isEmpty) ""
          else
        t.getSuperInterfaces.map(name(_)).mkString(", ", ", ", "")
          } {
        private static readonly long serialVersionUID = 0x5c11L + ("${t.getSkillName}".GetHashCode()) << 32;

        public override string skillName() {
            return "${t.getSkillName}";
        }

        /// <summary>
        /// Create a new unmanaged ${t.getName.capital()}. Allocation of objects without using the
        /// access factory method is discouraged.
        /// </summary>
        public ${name(t)}() : base(-1) {

        }

        /// <summary>
        /// Used for internal construction only!
        /// </summary>
        /// <param id=skillID></param>
        public ${name(t)}(int skillID) : base(skillID) {
        }
""")

	    if(!relevantFields.isEmpty){
        // TODO subtyping!
          out.write(s"""
        /// <summary>
        /// Used for internal construction, full allocation.
        /// </summary>
        public ${name(t)}(int skillID${appendConstructorArguments(t)}) : base(skillID) {
            ${relevantFields.map{f ⇒ s"this.${name(f)} = ${name(f)};"}.mkString("\n          ")}
        }
""")
	}
      
      if(visited.contains(t.getSkillName)){
          out.write(s"""
        public _R accept<_R, _A, _E>(Visitor<_R, _A, _E> v, _A arg) where _E : Exception {
            return v.visit(this, arg);
        }
""")
      }

      var implementedFields = t.getFields.toSeq
      def addFields(i : InterfaceType) {
        implementedFields ++= i.getFields.toSeq
        for(t <- i.getSuperInterfaces)
          addFields(t)
      }
      t.getSuperInterfaces.foreach(addFields)

	///////////////////////
	// getters & setters //
	///////////////////////
	for(f <- implementedFields) {
    def makeField:String = {
		  if(f.isIgnored){
		    ""
		  } else {
	      s"""
        ${if (f.isAuto())"[NonSerialized]" else ""}
        protected ${mapType(f.getType())} _${name(f)} = ${defaultValue(f)};
"""
	    }
    }

      def makeGetterImplementation:String = {
        if(f.isIgnored)
            s"""throw new Exception("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else
            s"get {return _${name(f)};}"
      }

      def makeSetterImplementation:String = {
        if(f.isIgnored)
            s"""throw new Exception("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else
            s"${ //@range check
//            if(f.getType().isInstanceOf[GroundType]){
//              if(f.getType().asInstanceOf[GroundType].isInteger)
//                f.getRestrictions.collect{case r:IntRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLow}L <= $Name && $Name <= ${r.getHigh}L, "$name has to be in range [${r.getLow};${r.getHigh}]"); """}.mkString("")
//              else if("f32".equals(f.getType.getName))
//                f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLowFloat}f <= $Name && $Name <= ${r.getHighFloat}f, "$name has to be in range [${r.getLowFloat};${r.getHighFloat}]"); """}.mkString("")
//              else if("f64".equals(f.getType.getName))
//               f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLowDouble} <= $Name && $Name <= ${r.getHighDouble}, "$name has to be in range [${r.getLowDouble};${r.getHighDouble}]"); """}.mkString("")
//              else
//                ""
//            }
//            else
//              ""
//          }${//@monotone modification check
//            if(!t.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty){
//              s"""assert skillID == -1L : "${t.getName} is specified to be monotone and this instance has already been subject to serialization!";
//        """
//            }
//            else
              ""
          }set {_${name(f)} = value;}"
      }

      if(f.isConstant)
          out.write(s"""
          ${comment(f)}static public ${mapType(f.getType())} ${name(f)} {
              get {
                  unchecked {
                      ${
                        f.getType.getSkillName match {
                          case "i8"          ⇒ s"return (sbyte)${f.constantValue().toString};"
                          case "i16"         ⇒ s"return (short)${f.constantValue().toString};"
                          case "i32"         ⇒ s"return ${f.constantValue().toString};"
                          case "i64" | "v64" ⇒ s"return ${f.constantValue().toString}L;"
                          case "f32"         ⇒ s"return ${f.constantValue().toString}f;"
                        }
                      }
                  }
              }
        }
""")
      else
            out.write(s"""$makeField
        ${comment(f)}public ${mapType(f.getType())} ${name(f)} {
            $makeGetterImplementation
            $makeSetterImplementation
        }
""")
    }
  
      // custom fields
      for(c <- customizations) {
        val mod = c.getOptions.toMap.get("modifier").map(_.head).getOrElse("public")
      
          out.write(s"""
        ${comment(c)}$mod ${c.`type`} ${name(c)};
""")
      }

      // fix toAnnotation
      if(!t.getSuperInterfaces.isEmpty())
          out.write(s"""
        public ${name(t)} self() {
            return this;
        }
""")

          out.write(s"""
        /// <summary>
        /// Generic sub types of this type.
        ///
        /// @author Simon Glaub, Timm Felden
        /// </summary>
        public new sealed class SubType : ${name(t)} , NamedType {
            private readonly AbstractStoragePool τPool;

            /// internal use only!!!
            public SubType(AbstractStoragePool τPool, int skillID) : base(skillID) {
                this.τPool = τPool;
            }

            public AbstractStoragePool ΤPool {
                get
                {
                    return τPool;
                }
            }

            public override string skillName() {
                return τPool.Name;
            }

            public override string ToString() {
                return skillName() + "#" + skillID;
            }
        }
    }
}
""");
      out.close()
    }
  }
}
