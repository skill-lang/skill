/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeHeader
    makeSource
  }
  private def makeHeader {
    val out = open("File.h")

    out.write(s"""
#include <skill/internal/SkillState.h>

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
namespace api {

/**
 * A skill file that corresponds to your specification. Have fun!
 *
 * @author Timm Felden
 */
class SkillFile : public ::skill::internal::SkillState {

        SkillFile(skill::streams::FileInputStream *in, const skill::api::WriteMode &mode,
                  skill::internal::StringPool *stringPool, skill::fieldTypes::AnnotationType *annotation,
                  std::vector<skill::internal::AbstractStoragePool *> *types, skill::api::typeByName_t *typesByName)
                : SkillState(in, mode, stringPool, annotation, types, typesByName) { }

/*(
  _path : Path,
  _mode : WriteMode,
  _String : StringPool,
  _annotationType : fieldTypes.AnnotationType,
  _types : ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
  _typesByName : HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]])
    extends SkillState(_path, _mode, _String, _annotationType, _types, _typesByName) {

  private[api] def AnnotationType = annotationType
${
      (for (t ← IR) yield s"""
  val ${name(t)} : internal.${storagePool(t)} = typesByName("${t.getSkillName}").asInstanceOf[internal.${storagePool(t)}]""").mkString
    }
}*/
public:

  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   *
   * TODO modes
   */
  static SkillFile* open(const std::string& path);

//  def create(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Create, write)

//  def read(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Read, write)
};
}${packageParts.map(_ ⇒ "}").mkString}
""")

    out.close()
  }

  private def makeSource {
    val out = open("File.cpp")

    out.write(s"""
#include "File.h"

using namespace $packageName::api;

static SkillFile *open(const std::string &path) {
}
""")

    out.close()
  }
}
