/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap

import de.ust.skill.ir.InterfaceType

trait TypesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for(t <- IR) {
      val out = files.open(s"${name(t)}.java")
      
      val customizations = t.getCustomizations.filter(_.language.equals("java")).toArray

      // package
      out.write(s"""package ${this.packageName};

${
  if(visited.contains(t.getSkillName)) s"""import ${this.packageName}.api.Visitor;
"""
  else ""
}import de.ust.skill.common.java.internal.NamedType;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.StoragePool;
${customizations.flatMap(_.getOptions.get("import")).map(i⇒s"import $i;\n").mkString}
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
}public class ${name(t)} extends ${
      if (null != t.getSuperType()) { name(t.getSuperType) }
      else { "SkillObject" }
      }${
      if(t.getSuperInterfaces.isEmpty) ""
      else
    t.getSuperInterfaces.map(name(_)).mkString(" implements ", ", ", "")
      } {
    private static final long serialVersionUID = 0x5c11L + ((long) "${t.getSkillName}".hashCode()) << 32;

    @Override
    public String skillName() {
        return "${t.getSkillName}";
    }

    /**
     * Create a new unmanaged ${t.getName.capital()}. Allocation of objects without using the
     * access factory method is discouraged.
     */
    public ${name(t)}() {
        super(-1);
    }

    /**
     * Used for internal construction only!
     * 
     * @param skillID
     */
    public ${name(t)}(int skillID) {
        super(skillID);
    }
""")

	    if(!relevantFields.isEmpty){
        // TODO subtyping!
    	  out.write(s"""
    /**
     * Used for internal construction, full allocation.
     */
    public ${name(t)}(int skillID${appendConstructorArguments(t)}) {
        super(skillID);
        ${relevantFields.map{f ⇒ s"this.${name(f)} = ${name(f)};"}.mkString("\n        ")}
    }
""")
	}
      
      if(visited.contains(t.getSkillName)){
        out.write(s"""
    public <_R, _A, _E extends Exception> _R accept(Visitor<_R, _A, _E> v, _A arg) throws _E {
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
    protected ${
		  if(f.isAuto()) "transient "
		  else ""
        }${mapType(f.getType())} ${name(f)} = ${defaultValue(f)};
"""
	    }
    }

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else
          s"return ${name(f)};"
      }

      def makeSetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
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
        }this.${name(f)} = ${name(f)};"
      }

      if(f.isConstant)
        out.write(s"""
    ${comment(f)}static public ${mapType(f.getType())} get${escaped(f.getName.capital)}() {
          ${
            f.getType.getSkillName match {
              case "i8"          ⇒ s"return (byte)${f.constantValue().toString};"
              case "i16"         ⇒ s"return (short)${f.constantValue().toString};"
              case "i32"         ⇒ s"return ${f.constantValue().toString};"
              case "i64" | "v64" ⇒ s"return ${f.constantValue().toString}L;"
              case "f32"         ⇒ s"return ${f.constantValue().toString}f;"
            }
          }
    }
""")
      else
        out.write(s"""$makeField
    ${comment(f)}final public ${mapType(f.getType())} get${escaped(f.getName.capital)}() {
        $makeGetterImplementation
    }

    ${comment(f)}final public void set${escaped(f.getName.capital)}(${mapType(f.getType())} ${name(f)}) {
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
    @Override
    public ${name(t)} self() {
        return this;
    }
""")

        out.write(s"""
    /**
     * Generic sub types of this type.
     * 
     * @author Timm Felden
     */
    public static final class SubType extends ${name(t)} implements NamedType {
        private final StoragePool<?, ?> τPool;

        /** internal use only!!! */
        public SubType(StoragePool<?, ?> τPool, int skillID) {
            super(skillID);
            this.τPool = τPool;
        }

        @Override
        public StoragePool<?, ?> τPool() {
          return τPool;
        }

        @Override
        public String skillName() {
            return τPool.name();
        }

        @Override
        public String toString() {
            return skillName() + "#" + skillID;
        }
    }
}
""");
      out.close()
    }
  }
}
