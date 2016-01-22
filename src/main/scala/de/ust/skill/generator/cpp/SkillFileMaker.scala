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

    out.write(s"""${beginGuard("file")}
#include <skill/fieldTypes/AnnotationType.h>
#include <skill/api/SkillFile.h>${
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
        struct SkillFile : public ::skill::api::SkillFile {

/*(
  _path : Path,
  _mode : WriteMode,
  _String : StringPool,
  _annotationType : fieldTypes.AnnotationType,
  _types : ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
  _typesByName : HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]])
    extends SkillState(_path, _mode, _String, _annotationType, _types, _typesByName) {*/
${
      (for (t ← IR) yield s"""
            ${storagePool(t)} *const ${name(t)};""").mkString
    }

            /**
             * !internal use only
             */
            SkillFile(skill::streams::FileInputStream *in, const skill::api::WriteMode &mode,
                      skill::internal::StringPool *stringPool, skill::fieldTypes::AnnotationType *annotation,
                      std::vector<std::unique_ptr<skill::internal::AbstractStoragePool>> *types,
                      skill::api::typeByName_t *typesByName)
                    : ::skill::api::SkillFile(in, mode, stringPool, annotation, types, typesByName)${
      (for (t ← IR) yield s""",
                      ${name(t)}((${storagePool(t)} *) annotation->type(${name(t)}::typeName))""").mkString
    } { }

            /**
             * Reads a binary SKilL file and turns it into a SKilL state.
             *
             * TODO modes
             */
            static SkillFile *open(const std::string &path);

//  def create(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Create, write)

//  def read(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Read, write)
        };
}${packageParts.map(_ ⇒ "}").mkString}
$endGuard""")

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
      (for (t ← IR; n = escaped(t.getSkillName)) yield s"""
        keeper->$n = pool->addLiteral(${name(t)}::typeName);""").mkString
    }${
      (for (s ← allStrings._2; name = escaped(s)) yield s"""
        keeper->$name = pool->addLiteral("$s");""").mkString
    }
        return pool;
    }

//!create a new pool in the target type system
static ::skill::internal::AbstractStoragePool *makePool(::skill::TypeID typeID,
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
    static ::skill::api::SkillFile *makeState(::skill::streams::FileInputStream *in,
                                              ::skill::WriteMode mode,
                                              ::skill::internal::StringPool *String,
                                              ::skill::fieldTypes::AnnotationType *Annotation,
                                              std::vector<std::unique_ptr<::skill::internal::AbstractStoragePool>> *types,
                                              ::skill::api::typeByName_t *typesByName,
                                              std::vector<std::unique_ptr<::skill::streams::MappedInStream>> &dataList) {

        auto &tbn = Annotation->init();
        const StringKeeper *const sk = (const StringKeeper *const) String->keeper;
        ::skill::api::String name;

        // ensure that pools exist at all${
      (for (t ← IR) yield s"""
        name = sk->${escaped(t.getSkillName)};
        if (!tbn[name->c_str()]) {
            const auto p = new ${storagePool(t)}((::skill::TypeID) types->size()${
        if (null == t.getSuperType) ""
        else s", tbn[sk->${escaped(t.getSuperType.getSkillName)}->c_str()]"
      }, name,
                                       new std::set<::skill::restrictions::TypeRestriction *>);
            tbn[name->c_str()] = p;
            types->push_back(std::unique_ptr<::skill::internal::AbstractStoragePool>(p));
            (*typesByName)[name] = p;
        }""").mkString
    }

        // trigger allocation and instance creation
        for (auto &t : *types) {
            t->allocateData();
            t->allocateInstances();
            if (nullptr == t->superPool)
                ::skill::internal::AbstractStoragePool::setNextPools(t.get());
        }

        ::skill::internal::triggerFieldDeserialization(types, dataList);

        return new $packageName::api::SkillFile(in, mode, String, Annotation, types, typesByName);
    }
${packageParts.map(_ ⇒ "}").mkString}
$packageName::api::SkillFile *$packageName::api::SkillFile::open(const std::string &path) {
    return ($packageName::api::SkillFile *) ::skill::internal::parseFile<$packageName::initializeStrings, $packageName::makePool, $packageName::makeState>(
            std::unique_ptr<::skill::streams::FileInputStream>(new ::skill::streams::FileInputStream(path)), ::skill::api::readOnly);
}
""")

    out.close()
  }
}
