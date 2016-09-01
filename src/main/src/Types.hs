module Types where

import Data.Word
import Data.Int
import Data.ByteString.Lazy
import Data.Binary.Get

type Ordered    = ([String], [TypeDesc], [String])
type BlockPair  = ([String], [TypeDesc])
type TypeDesc   = (String, Int, Maybe Int, Maybe Int, [FieldDesc]) --nameID, count, superID, LBPO, fieldDescriptors
type FieldDesc  = (Int, Maybe String, Maybe (Get Something), FieldData) --fieldID, name, getter, endOffset, fieldData
type FieldData  = ByteString

type Pointer = (Int, Int) -- skill name 'Annotation'
type UserType = String

data Something = CInt8   Int8             -- 0
               | CInt16  Int16            -- 1
               | CInt32  Int32            -- 2
               | CInt64  Int64            -- 3
               | CV64     Int64           -- 4
               | GPointer Pointer         -- 5
               | GBool    Bool            -- 6
               | GInt8    Int8            -- 7
               | GInt16   Int16           -- 8
               | GInt32   Int32           -- 9
               | GInt64   Int64           -- 10
               | GV64     Int64           -- 11
               | GFloat   Float           -- 12
               | GDouble  Double          -- 13
               | GString  Int             -- 14
               | GFArray  [Something]     -- 15
               | GVArray  Something       -- 17
               | GList    Something       -- 18
               | GSet     Something       -- 19
               | GMap Something Something -- 20
               -- TODO ?              -- 21+