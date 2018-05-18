/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.java.GeneralOutputMaker
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
 * @author Timm Felden
 * @note type access fields start with a capital letter to avoid collisions
 */
public static final class SkillState extends de.ust.skill.common.java.internal.SkillState implements SkillFile {

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
    public static SkillState open(Path path, Mode... mode) throws IOException, SkillException {
        ActualMode actualMode = new ActualMode(mode);
        try {
            switch (actualMode.open) {
            case Create:
                // initialization order of type information has to match file
                // parser
                // and can not be done in place
                StringPool strings = new StringPool(null);
                ArrayList<StoragePool<?, ?>> types = new ArrayList<>(${IR.size});
                Annotation annotation = new Annotation(types);

                return new SkillState(new HashMap<>(), strings, annotation,
                        types, FileInputStream.open(path, false), actualMode.close);

            case Read:
                Parser p = new Parser(FileInputStream.open(path, actualMode.close == Mode.ReadOnly));
                return p.read(SkillState.class, actualMode.close);

            default:
                throw new IllegalStateException("should never happen");
            }
        } catch (SkillException e) {
            // rethrow all skill exceptions
            throw e;
        } catch (Exception e) {
            throw new SkillException(e);
        }
    }

    public SkillState(HashMap<String, StoragePool<?, ?>> poolByName, StringPool strings,
            Annotation annotationType, ArrayList<StoragePool<?, ?>> types, FileInputStream in, Mode mode) {
        super(strings, in.path(), mode, types, poolByName, annotationType);

        try {
            StoragePool<?, ?> p;${
      (for (t ← IR)
        yield s"""
            ${name(t)}s = (null == (p = poolByName.get("${t.getSkillName}"))) ? Parser.newPool("${t.getSkillName}", ${
        if (null == t.getSuperType) "null"
        else s"${name(t.getSuperType)}s"
      }, types) : (${access(t)}) p;""").mkString("")
    }${
      (for (t ← types.getInterfaces)
        yield s"""
            ${name(t)}s = new ${interfacePool(t)}("${t.getSkillName}", ${
        if (t.getSuperType.getSkillName.equals("annotation")) "annotationType"
        else name(t.getSuperType) + "s";
      }${
        val realizations = collectRealizationNames(t);
        if (realizations.isEmpty) ""
        else realizations.mkString(",", ",", "")
      });""").mkString("")
    }
        } catch (ClassCastException e) {
            throw new ParseException(in, -1, e,
                    "A super type does not match the specification; see cause for details.");
        }
        for (StoragePool<?, ?> t : types)
            poolByName.put(t.name(), t);

        finalizePools(in);
    }
${
      (for (t ← IR)
        yield s"""
    private final ${access(t)} ${name(t)}s;

    @Override
    public ${access(t)} ${name(t)}s() {
        return ${name(t)}s;
    }
""").mkString("")
    }${
      (for (t ← types.getInterfaces)
        yield s"""
    private final ${interfacePool(t)} ${name(t)}s;

    @Override
    public ${interfacePool(t)} ${name(t)}s() {
        return ${name(t)}s;
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

    types.getUsertypes.filter(reaches).map(name(_) + "s").toSeq
  }
}
