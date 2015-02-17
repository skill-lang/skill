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
        super(poolIndex, "${t.getSkillName}", new HashSet<String>(Arrays.asList(new String[] { ${t.getFields.map{f⇒s""""${f.getSkillName}""""}.mkString(", ")} })));
    }
}
""")

      out.close()
    }
  }
}
