module Memory where

import qualified Data.Map as M
import Data.ByteString.Lazy
import Data.Int
import Data.Binary.Get
import System.IO.Unsafe
import Data.IORef

type State     = ([String], [String], [TD]) -- strings, uTs, tDs
data TD  = TD {
 typeID   :: TypeID,
 name     :: Name,
 count    :: Count,
 fDs      :: [FD],
 subtypes :: [TD],
 restr    :: Restr} deriving Show
 
type FD = (Name, [Something], FieldRecords)

{-# NOINLINE states #-}
states :: IORef [State]
states = unsafePerformIO $ newIORef []

----------------------------------------------------------------------------------------------------
-- The record types correspond to the information that, even though it has no barring on the data --
-- structure, is preserved during deserialization to be written back during serialization.        --
----------------------------------------------------------------------------------------------------
type FieldRecords = (RawData, TypeInfo, Restr)
type RawData      = ByteString
type TypeInfo     = ByteString
type M'TypeInfo   = Maybe ByteString
type Restr        = ByteString
type M'Restr      = Maybe ByteString
type Name         = String
type M'Name       = Maybe String
type Count        = Int
type Offset       = Int
type SuperID      = Int
type M'SuperID    = Maybe Int
type LBPO         = Int
type M'LBPO       = Maybe Int
type TypeID       = Int
type M'TypeID     = Maybe Int
type FieldID      = Int
type M'Getter     = Maybe (Get Something)
type Data         = ByteString
type FieldData    = ByteString
type Ref          = (Int, Int) -- skill name 'Annotation'
type UserType     = String
type NameID       = Int
type LFieldCount  = Int


data Something = CInt8   Int8                     -- 0
               | CInt16  Int16                    -- 1
               | CInt32  Int32                    -- 2
               | CInt64  Int64                    -- 3
               | CV64    Int64                    -- 4
               | GRef    Ref                      -- 5
               | GBool   Bool                     -- 6
               | GInt8   Int8                     -- 7
               | GInt16  Int16                    -- 8
               | GInt32  Int32                    -- 9
               | GInt64  Int64                    -- 10
               | GV64    Int64                    -- 11
               | GFloat  Float                    -- 12
               | GDouble Double                   -- 13
               | GString String                   -- 14
               | GFArray [Something]              -- 15
               | GVArray [Something]              -- 17
               | GList   [Something]              -- 18
               | GSet    [Something]              -- 19
               | GMap    [(Something, Something)] -- 20
               | GInt    Int                      -- selfmade for serialization
                     deriving Show

-- create a boxed version of a map
box_map t1 t2 = \values -> GMap $ Prelude.map (\(v1,v2) -> (t1 v1, t2 v2)) (M.assocs values)

-- create an unboxed version of a map
unbox_map t1 t2 = \(GMap values) -> M.fromList $ Prelude.map (\(v1,v2) -> (t1 v1, t2 v2)) values

instance Show (Get a) where
  show a = "some getter"

