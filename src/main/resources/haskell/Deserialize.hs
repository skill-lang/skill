module Deserialize where

import qualified Data.ByteString.Lazy.Char8 as C
import qualified Data.List as L
import System.IO
import System.IO.Unsafe
import Data.IORef
import Data.Int
import Data.Binary.Get
import Data.Char
import Data.Word
import ReadFields
import Methods
import Types

----------------------------------------------------------------------------------------------------------------------
-- This class processes an .sf file stores its contents internally. The method call is >> initialize filePath <<    --
----------------------------------------------------------------------------------------------------------------------
-- ## abbreviations ##                                                                                              --
-- r'    = read; used for the Get methods, e.g. r'strings is the name of the procedure that reads the string block  --
-- r_    = remaining; used for the remaining part of a list, e.g. (element : r_element) as a parameter              --
-- f_    = finished; used for already processed elements                                                            --
-- n_    = new; used for updated but not yet fully processed elements
-- tD(s) = typeDescriptor(s)                                                                                        --
-- fD(s) = fieldDescriptor(s)
-- bP(s) = blockPair(s)                                                                                       --
----------------------------------------------------------------------------------------------------------------------
-- sample filePath = "C:\\skill\\src\\test\\resources\\genbinary\\auto\\accept\\age.sf"                             --
----------------------------------------------------------------------------------------------------------------------

-- fourColoredNodes Error

type TD_v1 = (Maybe Int, String, Int, Maybe Int, Maybe Int, Int) -- id, name, count, superID, LBPO, LFieldCount
type FD_v1 = (Int, Maybe String, Maybe (Get Something), Int) --fieldID, name, getter, endOffset
type FD_v2 = (Int, Maybe String, Maybe (Get Something), FieldData) -- fieldID, maybe name, maybe getter, fieldData
type PartialState = ([String], [String], [TD_v2]) -- strings, uTs, tDs_v2
type State        = ([String], [String], [TypeDesc]) -- strings, uTs, tDs

{-# NOINLINE state #-}
state :: IORef State
state = unsafePerformIO $ newIORef ([],[],[]) -- trivial and inconsequential start value

initialize :: FilePath -> IO ()
initialize filePath = runGet process `fmap` C.readFile filePath >>= writeIORef state

initializeTest :: FilePath -> IO ()
initializeTest filePath = runGet process `fmap` C.readFile filePath >>= \res -> writeIORef state res >> printState res

process :: Get State
process = r'file ([], [], []) >>= \(strings, uTs, tDs) -> return (strings, uTs, remake tDs)
             where remake = createHierarchy . triggerGetters . compress


r'file :: PartialState -> Get PartialState
r'file (f_strings, f_uTs, f_tDs_v2) = isEmpty >>= \e -> if e then return (f_strings, f_uTs, f_tDs_v2)
 else do stringCount     <- readV64
         offsets         <- readOffsets stringCount
         strings         <- r'strings offsets                         -- end of string block
         let allStrings   = f_strings ++ strings
         typeCount       <- readV64
         (tDs_v1, uTs)   <- r'tDs_v1 f_uTs allStrings f_tDs_v2 typeCount
         fDs_v1          <- r'fDs_v1 allStrings f_tDs_v2 tDs_v1      -- end of descriptive information
         typeData        <- r'data $ (map . map) (\(_,_,_,offset) -> offset) fDs_v1
         return (allStrings, f_uTs ++ uTs, f_tDs_v2 ++ b'tDs_v2 tDs_v1 fDs_v1 typeData)

-- reads n many i32 values
readOffsets :: Int -> Get [Int32]
readOffsets = go 0
 where go _ 0 = return []
       go prevOffset i = getInt32be >>= \i32 -> go i32 (i-1) >>= \rest -> return (i32 - prevOffset : rest)

-- reads strings from the given offsets
r'strings :: [Int32] -> Get [String]
r'strings [] = return []
r'strings (offset : r_offsets)
 = getLazyByteString (fromIntegral offset) >>= \s -> r'strings r_offsets >>= \r_ss -> return (C.unpack s : r_ss)

-- reads the information about type descriptors from their position in the type block
r'tDs_v1 :: [String] -> [String] -> [TD_v2] -> Int -> Get ([TD_v1], [String])
r'tDs_v1 _ _ _ 0 = return ([],[])
r'tDs_v1 uTs strings f_tDs_v2 i
   = do name              <- readRefString strings
        let q1             = not $ any (\(_,name',_,_,_,_) -> name' == name) f_tDs_v2
        let n_uTs          = select q1 (uTs ++ [name])     uTs
        let id             = select q1 (Just (length uTs)) Nothing
        count             <- readV64
        _                 <- maybeSkipTypeRestrictions q1
        superID           <- maybeRead q1 readIndex
        let q3             = count > 0 && superID `isGreater` (-1)
        lBPO              <- maybeRead q3 readV64
        lFieldCount       <- readV64
        (r_tDs_v1, f_uTs) <- r'tDs_v1 n_uTs strings f_tDs_v2 (i-1)
        return ((id, name, count, superID, lBPO, lFieldCount) : r_tDs_v1, f_uTs)

-- reads field descriptors from the given offsets
r'fDs_v1 :: [String] -> [TD_v2] -> [TD_v1] -> Get [[FD_v1]]
r'fDs_v1 = go 0
  where go _ _ _ [] = return []
        go latestOffset strings allDescs ((_,name,_,_,_,fieldCount) : r_tDs)
            = do let f_instancesOfCurrentType = filter (\(_,name',_,_,_,_) -> name' == name) allDescs
                 let fDsOfCurrentType         = concatMap (\(_,_,_,_,_,fDs) -> fDs) f_instancesOfCurrentType
                 let fieldIDsOfCurrentType    = map (\(fieldID,_,_,_) -> fieldID) fDsOfCurrentType
                 (fDsForInstance, offset)     <- r'fDsOfType strings latestOffset fieldIDsOfCurrentType fieldCount
                 r_fDs_v1                     <- go offset strings allDescs r_tDs
                 return $ fDsForInstance : r_fDs_v1

r'fDsOfType :: [String] -> Int -> [Int] -> Int -> Get ([FD_v1], Int)
r'fDsOfType _ latestOffset _ 0 = return ([], latestOffset)
r'fDsOfType strings latestOffset allFieldIDsOfCurrentType fieldCount
  = do fieldID                    <- readV64
       let q2                     = fieldID `notElem` allFieldIDsOfCurrentType
       name                       <- maybeRead q2 (readRefString strings)
       getter                     <- maybeRead q2 parseTypeDescription
       _                          <- maybeSkipFieldRestrictions q2 getter
       offset                     <- readV64
       (r_fDsOfType, finalOffset) <- r'fDsOfType strings offset allFieldIDsOfCurrentType (fieldCount - 1)
       return ((fieldID, name, getter, offset - latestOffset) : r_fDsOfType, finalOffset)

-- read the data part of a type block. takes all (n*m) offsets as the argument
r'data :: [[Int]] -> Get [[FieldData]]
r'data [] = return []
r'data (offsets : r_offsets) = r'typeData offsets >>= \d -> r'data r_offsets >>= \r_d -> return (d : r_d)

-- reads the data corresponding to one type. takes its n offsets as the argument
r'typeData :: [Int] -> Get [FieldData]
r'typeData [] = return []
r'typeData (o : r_o) = getLazyByteString (fromIntegral o) >>= \d -> r'typeData r_o >>= \r_d -> return (d : r_d)

-- builds the next generation of type descriptors; stores field descriptors and data directly
-- e1 == id; e2 == name; e3 == count; e4 == superID; e5 == LBPO
b'tDs_v2 :: [TD_v1] -> [[FD_v1]] -> [[FieldData]] -> [TD_v2]
b'tDs_v2 [] [] [] = []
b'tDs_v2 ((e1,e2,e3,e4,e5, _) : r_tDs_v1) (fDs_v1 : r_fDs_v1) (typeData : r_d)
       = (e1,e2,e3,e4,e5, b'fDs_v2 fDs_v1 typeData) : b'tDs_v2 r_tDs_v1 r_fDs_v1 r_d

-- takes a list of the strings and a list of data chunks. attributes the data chunks correctly
b'fDs_v2 :: [FD_v1] -> [FieldData] -> [FD_v2]
b'fDs_v2 [] [] = []
b'fDs_v2 ((e1,e2,e3, _) : r_fDs_v1) (data' : r_data') = (e1,e2,e3, data') : b'fDs_v2 r_fDs_v1 r_data'

data TD_v5    = TD5 (Int, String, [FieldDescTest], [TD_v5], [(Int, Int)], Int, Int) -- id, name, fieldDescs, maybe subtypes, snips, firstIndex, baseFirstIndex
type TD_v4    = (Int, String, Int, Int, Maybe Int, [FieldDescTest]) -- id, name, count superID, maybe lbpo, fieldDescs
type TD_v3    = (Int, String, Int, Int, Maybe Int, [FD_v2]) -- id, name, count, superID, maybe lbpo, fD_v2s
type TD_v2    = (Maybe Int, String, Int, Maybe Int, Maybe Int, [FD_v2]) -- id, name, count, maybe superID, maybe lbpo, fieldDescs

-- merges type-extending descriptors with their originals, to create a 1 - 1 relation between types and descriptors
compress :: [TD_v2] -> [TD_v3]
compress = go []
  where go :: [TD_v3] -> [TD_v2] -> [TD_v3]
        go f_tDs [] = f_tDs
        go f_tDs ((Just id, e2, e3, Just superID, e5, e6) : r_tDs) = go (f_tDs ++ [(id, e2, e3, superID, e5, e6)]) r_tDs -- id exists -> original type
        go f_tDs ((Nothing, name, e3, e4, e5, e6) : r_tDs) = go f_tDs' r_tDs -- id doesn't exist => type has to be merged
              where f_tDs' = replace' index ((f_tDs !! index) `mergeTDs` (Nothing, name, e3, e4, e5, e6)) f_tDs
                    index  = findIndex' 0 name f_tDs
                       where findIndex' i name ((_,name',_,_,_,_) : r_tDs)
                                          | name == name' = i
                                          | otherwise     = findIndex' (i+1) name r_tDs

mergeTDs :: TD_v3 -> TD_v2 -> TD_v3 -- merges an original type desc with an extending type desc
mergeTDs (e1, e2, e3, e4, e5, fDs) (_, _, 0, _, _, fDs')         = (e1, e2, e3, e4, e5, fDs ++ fDs') -- no new objects
mergeTDs (e1, e2, count, e4, e5, fDs) (_, _, count', _, _, fDs') = (e1, e2, count + count', e4, e5, fDs `mergeFDs` fDs') -- new objects

mergeFDs :: [FD_v2] -> [FD_v2] -> [FD_v2]
-- TODO see if there are type descriptors with no field descriptors
mergeFDs [] fDs                       = fDs
mergeFDs fDs []                       = fDs
mergeFDs (fD_a : r_fDs_a) (fD_b : r_fDs_b) = merged : mergeFDs r_fDs_a r_fDs_b
    where merged = (\(e1,e2,e3,data') (_,_,_,data'') -> (e1, e2, e3, data' `a` data'')) fD_a fD_b

triggerGetters :: [TD_v3] -> [TD_v4]
triggerGetters = handleTypeDescs . filterNonEmpty
  where filterNonEmpty  = filter (\(e1, e2, count, e4, e5, e6)  -> count > 0)
        handleTypeDescs = map    (\(e1, e2, count, e4, e5, fDs) -> (e1, e2, count, e4, e5, map (go count) fDs))
             where go count (_, Just name, Just getter, data') = (name, runGet (repeatGet count getter) data', data')

createHierarchy :: [TD_v4] -> [TypeDesc]
createHierarchy typeDescs = map convertTD_v5 $ go ([], typeDescs)
   where go :: ([TD_v5], [TD_v4]) -> [TD_v5]
         go (f_tDs, []) = f_tDs
         go (f_tDs, tD : r_tDs) = go (f_tDs `integrate` tD, r_tDs)

-- takes a list of processed, hierarchically ordered type descs and the to-process next one and integrates it.
integrate :: [TD_v5] -> TD_v4 -> [TD_v5]
integrate f_tDs (id, e2, _, -1, _, e6) = f_tDs ++ [TD5 (id, e2, e6, [], [], 1, 1)]
integrate f_tDs tD                     = (\(Just a) -> a) (f_tDs `go` tD)
 where go :: [TD_v5] -> TD_v4 -> Maybe [TD_v5]
       go [] _        = Nothing -- right master not around -> FAILURE;
       go (TD5 (id, a2, a3, s_tDs, a5, a6, a7) : r_f_tDs) (id', e2, e3, supID, e5, e6)
        | id == supID = Just (TD5 (id, a2, a3, s_tDs, a5, a6, a7) `enslave` (id', e2, e3, supID, e5, e6) : r_f_tDs) -- first tD is master -> enslave
        | otherwise   = case s_tDs `go` (id', e2, e3, supID, e5, e6) -- first is not master -> try to give it to one of first's slaves
            of Just s_tDs' -> Just $ TD5 (id, a2, a3, s_tDs', a5,a6,a7) : r_f_tDs -- success -> return tD with updated slaves
               Nothing     -> Just $ TD5 (id, a2, a3, s_tDs, a5,a6,a7) : (r_f_tDs `integrate` (id,e2,e3, supID, e5,e6)) -- failure -> try next tD


-- takes a super and a sub type and stuffs the sub type into the children variable of the super type
enslave :: TD_v5 -> TD_v4 -> TD_v5
enslave (TD5 (id, name, fDs, tDs, snips, firstIndex, firstBaseIndex)) (id', name', count', _, Just lbpo', fDs')
      = TD5 (id, name, fd1, enslavedTD : tDs, (fixedLbpo, count') : snips, firstIndex, firstBaseIndex)
      where enslavedTD      = TD5 (id', name', fd2 ++ fDs', [], [], lbpo' + firstBaseIndex, firstBaseIndex)
            (fd1, fd2)      = remodel'' $ map (splitFieldDescs adjustedOffsets) fDs
            adjustedOffsets = adjustOffsets (fixedLbpo, count') snips
            fixedLbpo       = lbpo' - firstIndex + 1

adjustOffsets :: (Int, Int) -> [(Int, Int)] -> (Int, Int)
adjustOffsets (offset, count) snips = go (offset, count) $ filter (\(offset',_) -> offset' < offset) snips
  where go (offset, count) snips = (offset - sum (map snd snips), count)

splitFieldDescs :: (Int, Int) -> FieldDescTest -> (FieldDescTest, FieldDescTest)
splitFieldDescs (a,b) (name, data', rawData) = ((name, inner, rawData), (name, outer, rawData))
  where (inner, outer) = (subList a b data', take a data' ++ drop (a+b) data')

convertTD_v5 :: TD_v5 -> TypeDesc
convertTD_v5 (TD5 (id, name, e3, children, _, _, _)) = TD (id, name, e3, map convertTD_v5 children)