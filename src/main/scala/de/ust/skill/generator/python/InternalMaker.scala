package de.ust.skill.generator.python

import de.ust.skill.generator.python.FieldDeclarationMaker
import de.ust.skill.generator.python.FileParserMaker
import de.ust.skill.generator.python.PoolsMaker
import de.ust.skill.generator.python.StateMaker
import java.io.File
import java.security.ProtectionDomain


/*
 * Create an internal module
 */

trait InternalMaker extends FakeMain
    with FieldDeclarationMaker
    with FileParserMaker
    with PoolsMaker
    with StateMaker {

  abstract override def make {
    super.make

    val out = files.open(s"internal.py")

    out.write(s"""
from src.api.SkillFile import SkillFile
from src.api.SkillFile import ActualMode
from src.internal.FileParser import FileParser
from src.internal.SkillState import SkillState as State
from src.internal.Exceptions import SkillException
from src.internal.BasePool import BasePool
from src.internal.FieldDeclaration import FieldDeclaration
from src.internal.FieldType import FieldType
from src.internal.KnownDataField import KnownDataField
from src.internal.SkillObject import SkillObject
from src.internal.StoragePool import StoragePool
from src.internal.StringPool import StringPool
from src.internal.fieldDeclarations import AutoField
from src.internal.Exceptions import ParseException
from src.internal.KnownDataField import LazyField
from src.internal.fieldTypes.Annotation import Annotation
from src.internal.fieldTypes.BoolType import BoolType
from src.internal.fieldTypes.ConstantLengthArray import ConstantLengthArray
from src.internal.fieldTypes.ConstantTypes import ConstantI8
from src.internal.fieldTypes.ConstantTypes import ConstantI16
from src.internal.fieldTypes.ConstantTypes import ConstantI32
from src.internal.fieldTypes.ConstantTypes import ConstantI64
from src.internal.fieldTypes.ConstantTypes import ConstantV64
from src.internal.fieldTypes.FloatType import F32
from src.internal.fieldTypes.FloatType import F64
from src.internal.fieldTypes.IntegerTypes import I8
from src.internal.fieldTypes.IntegerTypes import I16
from src.internal.fieldTypes.IntegerTypes import I32
from src.internal.fieldTypes.IntegerTypes import I64
from src.internal.fieldTypes.IntegerTypes import V64
from src.internal.fieldTypes.ListType import ListType
from src.internal.fieldTypes.MapType import MapType
from src.internal.fieldTypes.ReferenceType import ReferenceType
from src.internal.fieldTypes.SetType import SetType
from src.internal.fieldTypes.SingleArgumentType import SingleArgumentType
from src.internal.fieldTypes.VariableLengthArray import VariableLengthArray
from src.internal.Blocks import *
from src.streams.FileInputStream import FileInputStream
from src.streams.MappedInStream import MappedInStream
from src.streams.MappedOutputStream import MappedOutputStream

${printAllFiles()}
""")

    makeState(out)
    makeParser(out)
    makePools(out)
    makeFields(out)

    out.write("""
""")

    out.close()
  }

    def printAllFiles(): String = {
        val dir = files.getOutPath
        var str = "\n"
        val fs = dir.listFiles()
        if (fs == null) return str
        val list = new Array[String](fs.length)
        for(i <- 0 until fs.length){
            list(i) = fs(i).getName
            var f = fs(i).getName
            if (f.endsWith(".py")) {
                f = f.substring(0, f.length - 3)
                if(!f.equals("internal")) {
                    val fileName = dir.getName + "." + f
                    str = str + s"""from $fileName import *\n"""
                }
            }
        }
        return str
    }
}

