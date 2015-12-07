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
#include <skill/internal/SkillState.h>${
      (for (t ← IR)
        yield s"""
#include "${storagePool(t)}.h"""").mkString
    }

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
namespace api {

/**
 * A skill file that corresponds to your specification. Have fun!
 *
 * @author Timm Felden
 */
class SkillFile : public ::skill::internal::SkillState {

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
         * !internal use only
         */
        SkillFile(skill::streams::FileInputStream *in, const skill::api::WriteMode &mode,
                  skill::internal::StringPool *stringPool, skill::fieldTypes::AnnotationType *annotation,
                  std::vector<std::unique_ptr<skill::internal::AbstractStoragePool>> *types,
                  skill::api::typeByName_t *typesByName)
                : SkillState(in, mode, stringPool, annotation, types, typesByName) { }

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
#include <skill/internal/UnknownBasePool.h>
#include <skill/internal/FileParser.h>
#include "File.h"
#include "StringKeeper.h"

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
    //! create the string pool
    static ::skill::internal::StringPool *initializeStrings(::skill::streams::FileInputStream *in) {
        auto keeper = new StringKeeper;
        ::skill::internal::StringPool *pool = new ::skill::internal::StringPool(in, keeper);${
      (for (s ← allStrings; name = escaped(s)) yield s"""
        keeper->$name = pool->add("$s");""").mkString
    }
        return pool;
    }

//!create a new pool in the target type system
static ::skill::internal::AbstractStoragePool *testPool(::skill::TypeID typeID,
                                               ::skill::api::String name,
                                               ::skill::internal::AbstractStoragePool *superPool,
                                               std::set<::skill::restrictions::TypeRestriction *> *restrictions,
                                               const ::skill::internal::AbstractStringKeeper *const keeper) {
${
      if (IR.isEmpty) ""
      else """    const StringKeeper *const sk = (const StringKeeper *const) keeper;"""
    }${
      (for (t ← IR)
        yield s"""
    if (name == sk->${escaped(t.getSkillName)}) {${
        if (null == t.getSuperType) s"""
        if (nullptr != superPool)
            throw ::skill::SkillException("the opened file contains a type ${name(t)} with super type, but none was expected");
        else
            return new ${storagePool(t)}(typeID, name, restrictions);"""
        else s"""
        if (nullptr == superPool)
            throw ::skill::SkillException("the opened file contains a type ${name(t)} with no super type, but ${name(t.getSuperType)} was expected");
        else if(superPool->name != sk->${escaped(t.getSuperType.getSkillName)})
            throw ::skill::SkillException("the opened file contains a type ${name(t)} with supertype different from ${name(t.getSuperType)}");
        else
            return new ${storagePool(t)}(typeID, superPool, name, restrictions);"""
      }
    }"""
      ).mkString
    }
    if (nullptr == superPool)
        return new ::skill::internal::UnknownBasePool(typeID, name, restrictions);
    else
        return superPool->makeSubPool(typeID, name, restrictions);
}

//! create a new state in the target type system
static ::skill::api::SkillFile *testMake(::skill::streams::FileInputStream *in,
                                          ::skill::WriteMode mode,
                                          ::skill::internal::StringPool *String,
                                          ::skill::fieldTypes::AnnotationType *Annotation,
                                          std::vector<std::unique_ptr<::skill::internal::AbstractStoragePool>> *types,
                                          ::skill::api::typeByName_t *typesByName,
                                          std::vector<std::unique_ptr<::skill::streams::MappedInStream>> &dataList) {
    //! TODO read field data

    // trigger allocation and instance creation
    for (auto &t : *types) {
        t->allocateData();
        t->allocateInstances();
        //if (nullptr==t->superPool)
        //  StoragePool.setNextPools(t);
    }

    return new $packageName::api::SkillFile(in, mode, String, Annotation, types, typesByName);
}
${packageParts.map(_ ⇒ "}").mkString}
$packageName::api::SkillFile *$packageName::api::SkillFile::open(const std::string &path) {
    return ($packageName::api::SkillFile *) ::skill::internal::parseFile<$packageName::initializeStrings, $packageName::testPool, $packageName::testMake>(
            std::unique_ptr<::skill::streams::FileInputStream>(new ::skill::streams::FileInputStream(path)), ::skill::api::readOnly);
}
""")

    out.close()
  }
}
