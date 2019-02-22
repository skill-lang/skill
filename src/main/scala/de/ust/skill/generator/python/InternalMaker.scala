package de.ust.skill.generator.python

import de.ust.skill.generator.python.{FieldDeclarationMaker, FileParserMaker, PoolsMaker, StateMaker}


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
from src.internal.fieldTypes.Annotation import Annotation
from src.internal.fieldTypes.BoolType import BoolType
from src.internal.fieldTypes.ConstantLengthArray import ConstantLegnthArray
from src.internal.fieldTypes.ConstantTypes import ConstantTypes
from src.internal.fieldTypes.FloatType import FloatType
from src.internal.fieldTypes.IntegerTypes import IntegerTypes
from src.internal.fieldTypes.ListType import ListType
from src.internal.fieldTypes.MapType import MapType
from src.internal.fieldTypes.ReferenceType import ReferenceType
from src.internal.fieldTypes.SetType import SetType
from src.internal.fieldTypes.SingleArgumentType import SingleArgumentType
from src.internal.fieldTypes.VariableLengthArray import VariableLengthArray
from src.internal.parts.Block import *
from src.streams.FileInputStream import FileInputStream
from src.streams.MappedInStream import MappedInStream
from src.streams.MappedOutStream import MappedOutStream
""")

    makeState(out)
    makeParser(out)
    makePools(out)
    makeFields(out)

    out.write("""
""")

    out.close()
  }
}