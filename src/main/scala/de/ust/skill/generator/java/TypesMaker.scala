/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java

import java.io.PrintWriter

import scala.collection.JavaConversions._

import de.ust.skill.ir._
import de.ust.skill.ir.restriction._

trait TypesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for(t <- IR){
      val out = open(name(t)+".java")

    //package
    out.write(s"""package ${this.packageName};

import de.ust.skill.common.java.internal.FieldDeclaration;
import de.ust.skill.common.java.internal.NamedType;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.StoragePool;
""")

    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1) else this.packageName;

      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      out.write(s"""
${
        comment(t)
}public class ${name(t)} extends ${
        if (null != t.getSuperType()) { name(t.getSuperType) }
        else { "SkillObject" }
      } {

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
    public ${name(t)}(long skillID) {
        super(skillID);
    }
""")

	if(!relevantFields.isEmpty){
    // TODO subtyping!
    	out.write(s"""
    /**
     * Used for internal construction, full allocation.
     */
    public ${name(t)}(long skillID${appendConstructorArguments(t)}) {
        super(skillID);
        ${relevantFields.map{f ⇒ s"this.${name(f)} = ${name(f)};"}.mkString("\n    ")}
    }
""")
	}

	///////////////////////
	// getters & setters //
	///////////////////////
	for(f <- t.getFields if !f.isInstanceOf[View]){
      def makeField:String = {
		if(f.isIgnored)
		  ""
		else
	      s"""
    protected ${mapType(f.getType())} ${name(f)} = ${defaultValue(f)};
"""
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
              ""
          }${//@monotone modification check
            if(!t.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty){
              s"""require(skillID == -1L, "${t.getName} is specified to be monotone and this instance has already been subject to serialization!"); """
            }
            else
              ""
        }this.${name(f)} = ${name(f)};"
      }

      if(f.isConstant)
        out.write(s"""
    ${comment(f)}static public ${mapType(f.getType())} get${f.getName.capital}() {
          ${
            f.getType.getSkillName match {
              case "i64" | "v64" ⇒ s"return ${f.constantValue().toString}L;"
              case "f32"         ⇒ s"return ${f.constantValue().toString}f;"
              case _             ⇒ s"return ${f.constantValue().toString};"
            }
          }
    }
""")
      else
        out.write(s"""$makeField
    ${comment(f)}final public ${mapType(f.getType())} get${f.getName.capital}() {
        $makeGetterImplementation
    }

    ${comment(f)}final public void set${f.getName.capital}(${mapType(f.getType())} ${name(f)}) {
        $makeSetterImplementation
    }
""")
    }

    // generic get
    locally{
      val fields = t.getFields.filter(!_.isIgnored)
      if(!fields.isEmpty)
        out.write(s"""
    /**
     * unchecked conversions are required, because the Java type system known
     * nothing of our invariants
     * 
     * @note to self: Boxing bei primitiven beachten!
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(FieldDeclaration<T, ?> field) {
        switch (field.name()) {${
          (for(f <- fields)
            yield s"""
        case "${f.getSkillName}":
            return (T) (${mapType(f.getType, true)}) ${
              if(f.isConstant()) "get" + f.getName.capital + "()"
              else name(f)
              };""").mkString
        }
        default:
            return super.get(field);
        }
    }
""")
    }

      // pretty string
    out.write(s"""
    /**
     * potentially expensive but more pretty representation of this instance.
     */
    @Override
    public String prettyString() {
        StringBuilder sb = new StringBuilder("${name(t)}(this: ").append(this);""")
    for(f <- t.getAllFields) out.write(
      if(f.isIgnored) s"""
        sb.append(", ${name(f)}: <<ignored>>");"""
      else if (!f.isConstant) s"""
        sb.append(", ${if(f.isAuto)"auto "else""}${name(f)}: ").append(${name(f)});"""
      else s"""
        sb.append(", const ${name(f)}: ${f.constantValue()}");"""
    )
    out.write("""
        return sb.append(")").toString();
    }
""")

    val prettyStringArgs = (for(f <- t.getAllFields)
      yield if(f.isIgnored) s"""+", ${f.getName()}: <<ignored>>" """
      else if (!f.isConstant) s"""+", ${if(f.isAuto)"auto "else""}${f.getName()}: "+_${f.getName()}"""
      else s"""+", const ${f.getName()}: ${f.constantValue()}""""
      ).mkString(""""(this: "+this""", "", """+")"""")

      out.write(s"""
    /**
     * Generic sub types of this type.
     * 
     * @author Timm Felden
     */
    public static final class SubType extends ${name(t)} implements NamedType {
        private final StoragePool<?, ?> τPool;

        SubType(StoragePool<?, ?> τPool, long skillID) {
            super(skillID);
            this.τPool = τPool;
        }

        @Override
        public String τName() {
            return τPool.name();
        }

        @Override
        public String toString() {
            return τName() + "#" + skillID;
        }
    }
}
""");
    out.close()
    }
  }
}
