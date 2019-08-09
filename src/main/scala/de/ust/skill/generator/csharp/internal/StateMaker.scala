/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.csharp.GeneralOutputMaker
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.Typedef
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.io.PrintWriter

trait StateMaker extends GeneralOutputMaker {
  final def makeState(out : PrintWriter) {

    out.write(s"""
        /**
         * Internal implementation of SkillFile.
         *
         * @author Simon Glaub, Timm Felden
         * @note type access fields start with a capital letter to avoid collisions
         */
        public sealed class SkillState : SkillFile {

            /**
             * Create a new skill file based on argument path and mode.
             *
             * @throws IOException
             *             on IO and mode related errors
             * @throws SkillException
             *             on file or specification consistency errors
             * @note suppress unused warnings, because sometimes type declarations are
             *       created, although nobody is using them
             */
            public static new SkillFile open(string path, params Mode[] mode) {
                ActualMode actualMode = new ActualMode(mode);
                try {
                    switch (actualMode.open) {
                    case Mode.Create:
                        // initialization order of type information has to match file
                        // parser
                        // and can not be done in place
                        StringPool strings = new StringPool(null);
                        List<AbstractStoragePool> types = new List<AbstractStoragePool>(1);
                        StringType stringType = new StringType(strings);
                        Annotation annotation = new Annotation(types);

                        return new SkillState(new Dictionary<string, AbstractStoragePool>(), strings, stringType, annotation,
                                types, FileInputStream.open(path, false), actualMode.close);

                    case Mode.Read:
                        Parser p = new Parser(FileInputStream.open(path, actualMode.close == Mode.ReadOnly));
                        return p.read<SkillState>(typeof(SkillState), actualMode.close);

                    default:
                        throw new System.InvalidOperationException("should never happen");
                    }
                } catch (SkillException e) {
                    // rethrow all skill exceptions
                    throw e;
                } catch (Exception e) {
                    throw new SkillException(e);
                }
            }

            public SkillState(Dictionary<string, AbstractStoragePool> poolByName, StringPool strings, StringType stringType,
                    Annotation annotationType, List<AbstractStoragePool> types, FileInputStream @in, Mode mode) : base(strings, @in.Path, mode, types, poolByName, stringType, annotationType) {

                try {
                    AbstractStoragePool p = null;${
              (for (t ← IR)
                yield s"""
                    poolByName.TryGetValue("${t.getSkillName}", out p);
                    ${name(t)}sField = (null == p) ? (${access(t)})Parser.newPool("${t.getSkillName}", ${
                if (null == t.getSuperType) "null"
                else s"${name(t.getSuperType)}sField"
              }, types) : (${access(t)}) p;""").mkString("")
            }${
              (for (t ← types.getInterfaces)
                yield s"""
                    ${name(t)}sField = new ${interfacePool(t)}("${t.getSkillName}", ${
                if (t.getSuperType.getSkillName.equals("annotation")) "annotationType"
                else name(t.getSuperType) + "sField";
              }${
                val realizations = collectRealizationNames(t);
                if (realizations.isEmpty) ""
                else realizations.mkString(", ", s", ", "")
              });""").mkString("")
            }
                } catch (System.InvalidCastException e) {
                    throw new ParseException(@in, -1, e,
                            "A super type does not match the specification; see cause for details.");
                }
                foreach (AbstractStoragePool t in types)
                    poolByName[t.Name] = t;

                finalizePools(@in);
                @in.close();
            }
        ${
              (for (t ← IR)
                yield s"""
            internal readonly ${access(t)} ${name(t)}sField;

            public override ${access(t)} ${name(t)}s() {
                return ${name(t)}sField;
            }
        """).mkString("")
            }${
              (for (t ← types.getInterfaces)
                yield s"""
            internal readonly ${interfacePool(t)} ${name(t)}sField;

            public override ${interfacePool(t)} ${name(t)}s() {
                return ${name(t)}sField;
            }
        """).mkString("")
            }}
""")

  }

  private def collectRealizationNames(target : InterfaceType) : Seq[String] = {
    def reaches(t : Type) : Boolean = t match {
      case t : UserType      ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : InterfaceType ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : Typedef       ⇒ reaches(t.getTarget)
      case _                 ⇒ false
    }

    types.getUsertypes.filter(reaches).map(name(_) + "sField").toSeq
  }
}
