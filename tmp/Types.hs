module Types where

import Data.Word
import Data.Int
import Data.ByteString.Lazy
import Data.Binary.Get

data TypeDesc  = TD (Int, String, [FieldDescTest], [TypeDesc]) -- id, name, fieldDescs, subtypes
type FieldDesc = (String, [Something]) --name, data
type FieldDescTest = (String, [Something], ByteString) --name, data, rawData

type FieldData = ByteString
type Pointer   = (Int, Int) -- skill name 'Annotation'
type UserType  = String

data Something = CInt8   Int8                  -- 0
               | CInt16  Int16                 -- 1
               | CInt32  Int32                 -- 2
               | CInt64  Int64                 -- 3
               | CV64     Int64                -- 4
               | GPointer Pointer              -- 5
               | GBool    Bool                 -- 6
               | GInt8    Int8                 -- 7
               | GInt16   Int16                -- 8
               | GInt32   Int32                -- 9
               | GInt64   Int64                -- 10
               | GV64     Int64                -- 11
               | GFloat   Float                -- 12
               | GDouble  Double               -- 13
               | GString  Int                  -- 14
               | GFArray  [Something]          -- 15
               | GVArray  [Something]          -- 17
               | GList    [Something]          -- 18
               | GSet     [Something]          -- 19
               | GMap [(Something, Something)] -- 20
               | GUserType Int Int
                     deriving (Show)

-- fa :: Int8
-- fb :: Int16
-- fc :: Int32
-- fd :: A
-- fe :: B
-- ff :: A

-- A
-- A <- B
-- A <- C
-- A <- D
-- E <- B
-- F <- B
-- G <- E
-- H <- F


data A = A' I_A | B' I_B | C' I_C | D' I_D
type I_A = Int8
type I_B = (Int16, Int8)
type I_C = (Int32, Int8)
type I_D = (A, Int8)







--class A' a where
  --g'fA :: a -> Int8
  --g'fB :: a -> Int16
--  g'fC :: a -> Int32
--  g'fD :: a ->

--instance A' A where
--getA i8 = i8

--class B x where
--  getB :: x -> B

--instance A B where
 --getA