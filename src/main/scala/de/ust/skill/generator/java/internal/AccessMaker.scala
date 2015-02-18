/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.restriction.SingletonRestriction

trait AccessMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR) {
      val isBasePool = (null == t.getSuperType)
      val nameT = name(t)

      val out = open(s"internal/${nameT}Access.java")
      //package & imports
      out.write(s"""package ${packagePrefix}internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import de.ust.skill.common.java.internal.BasePool;
import de.ust.skill.common.java.internal.FieldDeclaration;
import de.ust.skill.common.java.internal.FieldType;
import de.ust.skill.common.java.internal.StoragePool;
import de.ust.skill.common.java.internal.TypeMissmatchError;
import de.ust.skill.common.java.restrictions.FieldRestriction;

import ${packagePrefix}*;
""")

      //class declaration
      out.write(s"""
${
        comment(t)
      }public class ${nameT}Access extends ${
        if (isBasePool) s"BasePool<${nameT}>"
        else s"SubPool<${nameT}, ${name(t.getBaseType)}>"
      } {
${
        if (isBasePool) s"""
    // TODO optimize this method away by replacing empty arrays by null pointers
    @Override
    protected $nameT[] emptyArray() {
        return new $nameT[0];
    }"""
        else ""
      }
    /**
     * Can only be constructed by the SkillFile in this package.
     */
    ${nameT}Access(long poolIndex) {
        super(poolIndex, "${t.getSkillName}", new HashSet<String>(Arrays.asList(new String[] { ${t.getFields.map { f ⇒ s""""${f.getSkillName}"""" }.mkString(", ")} })));
    }

    @Override
    public boolean insertInstance(int skillID) {
        int i = skillID - 1;
        if (null != data[i])
            return false;

        $nameT r = new $packagePrefix$nameT(skillID);
        data[i] = r;
        staticData.add(r);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addKnownField(String name) {
        final FieldDeclaration<?, $nameT> f;
        switch (name) {${
          (for(f <- t.getFields)
            yield s"""
        case "${f.getSkillName}":
            f = new KnownField_${nameT}_${name(f)}(fields.size(), this);
            break;
"""
            ).mkString
        }
        default:
            super.addKnownField(name);
            return;
        }
        f.eliminatePreliminaryTypes((ArrayList<StoragePool<?, ?>>) owner.allTypes());
        fields.add(f);
    }

    /**
     * @return a new $nameT instance with default field values
     */
    public $nameT make() {
        $nameT rval = new $nameT();
        add(rval);
        return rval;
    }

    /**
     * @return a new age instance with the argument field values
     */
    public $nameT make(${makeConstructorArguments(t)}) {
        $nameT rval = new $nameT(-1${appendConstructorArguments(t, false)});
        add(rval);
        return rval;
    }

    public ${nameT}Builder build() {
        return new ${nameT}Builder(this, new $nameT());
    }

    /**
     * Builder for new $nameT instances.
     * 
     * @author Timm Felden
     */
    public static final class ${nameT}Builder extends Builder<$nameT> {

        protected ${nameT}Builder(StoragePool<$nameT, ? super $nameT> pool, $nameT instance) {
            super(pool, instance);
        }${
        (for (f ← t.getAllFields if !f.isIgnored() && !f.isConstant())
          yield s"""

        public ${nameT}Builder ${name(f)}(${mapType(f.getType)} ${name(f)}) {
            instance.set${f.getName.capital()}(${name(f)});
            return this;
        }""").mkString
      }
    }
}
""")

      out.close()
    }
  }
}
