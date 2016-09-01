module Deserialize where

import System.IO as IO
import Data.ByteString.Lazy.Char8 as C
import Data.List as L
import Data.ByteString.Lazy as S
import Prelude as P
import Data.Int
import Data.Binary.Get
import Data.Char
import Data.Word
import ReadFields
import Methods
import Types
import System.IO.Unsafe
import Data.IORef

------------------------------------------------------------------------------------------------------------------------
-- >> abbreviations <<
-- desc             = descriptor; used for type and field descriptors
-- prev             = previous; references elements that have already been read
-- something''      = non-final version of that something. Is usually missing values that are not yet available
-- r_element        = remaining list of that element, e.g. r_offsets are all but the first element in a list of offsets
-- tDs / fDs / strs = typeDescriptors / fieldDescriptors / strings; used when lines get long
-- something'       = different instance of an element of the same kind; used e.g. when merging two tupels of the same type
------------------------------------------------------------------------------------------------------------------------
-- sample filePath = "C:\\skill\\src\\test\\resources\\genbinary\\auto\\accept\\age.sf"
------------------------------------------------------------------------------------------------------------------------

type TypeDesc''  = (Int, Int, Maybe Int, Maybe Int, Int) --nameID, count, superID, LBPO, LFieldCount
type FieldDesc'' = (Int, Maybe Int, Maybe (Get Something), Int) --fieldID, name, getter, endOffset

{-# NOINLINE ordered #-}
ordered :: IORef Ordered
ordered = unsafePerformIO $ newIORef ([],[],[]) -- trivial and inconsequential start value

initialize :: FilePath -> IO ()
initialize filePath = do (strings, typeDescs) <- runGet process `fmap` C.readFile filePath
                         let userTypes         = L.map (\(name,_,_,_,_) -> name) typeDescs
                         writeIORef ordered (strings, typeDescs, userTypes)
                         printOrdered (strings, typeDescs, userTypes)

process :: Get ([String], [TypeDesc])
process = L.foldr (\(ss, tDs) (ss', tDs') -> (ss ++ ss', tDs ++ tDs')) ([], []) `fmap` go [] -- concat both halves
  where go prevBlocks = isEmpty >>= \e -> if e then return prevBlocks
                                               else readBlockPair prevBlocks >>= \p -> go (p : prevBlocks)

readBlockPair :: [BlockPair] -> Get BlockPair
readBlockPair prevBlocks
  = do stringCount      <- readV64
       offsets          <- readOffsets stringCount
       strings          <- readStrings offsets                         -- end of string block
       typeCount        <- readV64
       let prevTypeDescs = L.concatMap snd prevBlocks
       typeDescs''      <- readTypeDescs'' strings prevTypeDescs typeCount
       fieldDescPs      <- readFieldDescs'' strings prevTypeDescs typeDescs''      -- end of descriptive information
       typeData         <- readData $ (L.map . L.map) (\(_,_,_,a) -> a) fieldDescPs
       return (strings, buildTypeDescs strings typeDescs'' fieldDescPs typeData)

-- builds a "real" type descriptor; stores field descriptors and data directly
buildTypeDescs :: [String] -> [TypeDesc''] -> [[FieldDesc'']] -> [[FieldData]] -> [TypeDesc]
buildTypeDescs _ [] [] [] = []
buildTypeDescs strs ((nameID, e2,e3,e4, _) : r_TD) (fieldDescsForType'' : r_FD) (typeData : r_D)
 = (strs !! nameID, e2,e3,e4, buildFieldDescs strs fieldDescsForType'' typeData) : buildTypeDescs strs r_TD r_FD r_D

     --e2 == count
     --e3 == superID
     --e4 == LBPO

-- takes a list of the strings, a list of all field descriptors'' and a list of data chunks.
-- attributes the data chunks to their respective field descriptors and replaces name IDS with names
buildFieldDescs :: [String] -> [FieldDesc''] -> [FieldData] -> [FieldDesc]
buildFieldDescs strings fieldDescs'' data'
 = fst $ L.foldr (\(e1, nID, e3,_) (fDs, d:r_d) -> ((e1, strings `pick` nID, e3, d) : fDs, r_d)) ([],data') fieldDescs''


-- reads n many i32 values
readOffsets :: Int -> Get [Int32]
readOffsets = go 0
 where go _ 0 = return []
       go prevOffset i = getInt32be >>= \i32 -> go i32 (i-1) >>= \rest -> return (i32 - prevOffset : rest)

-- reads strings from the given offsets
readStrings :: [Int32] -> Get [String]
readStrings [] = return []
readStrings (offset : restO) = do string  <- getLazyByteString $ fromIntegral offset
                                  rest'   <- readStrings restO
                                  return $ C.unpack string : rest'

-- reads the information about type descriptors from their position in the type block
readTypeDescs'' :: [String] -> [TypeDesc] -> Int -> Get [TypeDesc'']
readTypeDescs'' _ _ 0 = return []
readTypeDescs'' strings prevTypeDescs i
  = do nameID        <- readV64
       let q1         = not $ L.foldl (\bool (name,_,_,_,_) -> bool || name == (strings !! nameID)) False prevTypeDescs -- yields true if nameID appeared before
       count         <- readV64
       _             <- maybeSkipTypeRestrictions q1
       superID       <- maybeReadV64 q1
       let q3         = count > 0 && superID `isGreater` 0 -- (?)
       lBPO          <- maybeReadV64 q3
       lFieldCount   <- readV64
       r_typeDescs'' <- readTypeDescs'' strings prevTypeDescs (i-1)
       return $ (nameID - 1, count, superID, lBPO, lFieldCount) : r_typeDescs'' -- our id's start at 0

-- reads field descriptors from the given offsets
readFieldDescs'' :: [String] -> [TypeDesc] -> [TypeDesc''] -> Get [[FieldDesc'']]
readFieldDescs'' _ _ [] = return []
readFieldDescs'' strings allDescs ((nameID,_,_,_,fieldCount) : restD)
  = do let prevInstancesOfCurrentType = L.filter (\(name,_,_,_,_) -> name == (strings !! nameID)) allDescs
       let descsOfCurrentType         = L.concatMap (\(_,_,_,_,e) -> e) prevInstancesOfCurrentType
       let fieldIDsOfCurrentType      = L.map (\(e,_,_,_) -> e) descsOfCurrentType
       fieldDescsForInstance         <- readFieldDescPsOfType fieldIDsOfCurrentType fieldCount
       r_fieldDescs''                <- readFieldDescs'' strings allDescs restD
       return $ fieldDescsForInstance : r_fieldDescs''


readFieldDescPsOfType :: [Int] -> Int -> Get [FieldDesc'']
readFieldDescPsOfType = go 0 -- variable to remodel offset structure: 4, 10, 12 -> 4, 6, 2j
 where go _ _ 0 = return []
       go latestOffset allFieldIDsOfCurrentType fieldCount
          = do fieldID  <- readV64
               let q2   = fieldID `L.notElem` allFieldIDsOfCurrentType
               nameID   <- maybeReadV64 q2
               getter   <- maybeReadTypeDescriptor q2
               _        <- maybeSkipFieldRestrictions q2 getter
               offset   <- readV64
               r_fDoT'' <- go offset allFieldIDsOfCurrentType (fieldCount - 1)
               return $ (fieldID, nameID `sub` 1, getter, offset - latestOffset) : r_fDoT'' -- our id's start at 0

-- read the data part of a type block. takes all (n*m) offsets as the argument
readData :: [[Int]] -> Get [[FieldData]]
readData [] = return []
readData (offsets : r_offsets) = do typeData   <- readTypeData offsets
                                    r_typeData <- readData $ (L.map . L.map) (\o -> o - L.last offsets) r_offsets
                                    return (typeData : r_typeData)

-- reads the data corresponding to one type. takes its n offsets as the argument
readTypeData :: [Int] -> Get [FieldData]
readTypeData [] = return []
readTypeData (offset : r_offsets) = do fieldData   <- getLazyByteString $ fromIntegral offset
                                       r_fieldData <- readTypeData (L.map (\o -> o - offset) r_offsets)
                                       return (fieldData : r_fieldData)