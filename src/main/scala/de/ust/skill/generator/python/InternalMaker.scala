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
from common.internal.FileParser import FileParser
from common.internal.SkillState import SkillState as State
from common.internal.Exceptions import SkillException, ParseException
from common.internal.BasePool import BasePool
from common.internal.FieldDeclaration import FieldDeclaration
from common.internal.FieldType import FieldType
from common.internal.KnownDataField import KnownDataField
from common.internal.SkillObject import SkillObject
from common.internal.StoragePool import StoragePool
from common.internal.StringPool import StringPool
from common.internal.AutoField import AutoField
from common.internal.LazyField import LazyField
from common.internal.fieldTypes.Annotation import Annotation
from common.internal.fieldTypes.BoolType import BoolType
from common.internal.fieldTypes.ConstantLengthArray import ConstantLengthArray
from common.internal.fieldTypes.ConstantTypes import ConstantI8, ConstantI16, ConstantI32, ConstantI64, ConstantV64
from common.internal.fieldTypes.FloatType import F32, F64
from common.internal.fieldTypes.IntegerTypes import I8, I16, I32, I64, V64
from common.internal.fieldTypes.ListType import ListType
from common.internal.fieldTypes.MapType import MapType
from common.internal.fieldTypes.SetType import SetType
from common.internal.fieldTypes.SingleArgumentType import SingleArgumentType
from common.internal.fieldTypes.VariableLengthArray import VariableLengthArray
from common.internal.Blocks import *
from common.streams.FileInputStream import FileInputStream
from common.streams.MappedInStream import MappedInStream
from common.streams.MappedOutputStream import MappedOutputStream
from common.internal.Mode import ActualMode, Mode
from common.internal.NamedType import NamedType

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

