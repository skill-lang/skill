module Memory where

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


instance Show (Get a) where
  show a = "some getter"

--instance NFData TypeDesc where
--        rnf (TD x1) = rnf x1 `seq` ()
--
--instance NFData Something where
--        rnf (CInt8 x1) = rnf x1 `seq` ()
--        rnf (CInt16 x1) = rnf x1 `seq` ()
--        rnf (CInt32 x1) = rnf x1 `seq` ()
--        rnf (CInt64 x1) = rnf x1 `seq` ()
--        rnf (CV64 x1) = rnf x1 `seq` ()
--        rnf (GRef x1) = rnf x1 `seq` ()
--        rnf (GBool x1) = rnf x1 `seq` ()
--        rnf (GInt8 x1) = rnf x1 `seq` ()
--        rnf (GInt16 x1) = rnf x1 `seq` ()
--        rnf (GInt32 x1) = rnf x1 `seq` ()
--        rnf (GInt64 x1) = rnf x1 `seq` ()
--        rnf (GV64 x1) = rnf x1 `seq` ()
--        rnf (GFloat x1) = rnf x1 `seq` ()
--        rnf (GDouble x1) = rnf x1 `seq` ()
--        rnf (GString x1) = rnf x1 `seq` ()
--        rnf (GFArray x1) = rnf x1 `seq` ()
--        rnf (GVArray x1) = rnf x1 `seq` ()
--        rnf (GList x1) = rnf x1 `seq` ()
--        rnf (GSet x1) = rnf x1 `seq` ()
--        rnf (GMap x1) = rnf x1 `seq` ()
--        rnf (GInt x1) = rnf x1 `seq` ()
--
--instance NFData ByteString where
--   rnf b = rnf b `seq` ()
--
--instance NFData (IO a) where
--   rnf a = rnf a `seq` ()
