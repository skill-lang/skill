module Deserialize where

import qualified Data.ByteString.Lazy.Char8 as C
import qualified Data.ByteString.Lazy as B
import qualified Data.ByteString.Lazy.UTF8 as UTF8
import Data.IORef
import System.IO
import Data.Int
import Data.Binary.Get
import ReadFields
import Methods
import Memory
import ImpossibleImports

----------------------------------------------------------------------------------------------------------------------
-- This class processes an .sf file and stores its contents internally. The method call is > deserialize filePath < --
----------------------------------------------------------------------------------------------------------------------
-- ## abbreviations ##                                                                                              --
-- g'    = get; used for self-written Get functions, e.g. g'strings for the function that parses the string block   --
-- r_    = remaining; used for the remaining part of a list, e.g. (element : r_element) as a parameter              --
-- f_    = finished; used for already processed elements                                                            --
-- n_    = new; used for updated but not yet fully processed elements                                               --
-- tD(s) = typeDescriptor(s)                                                                                        --
-- fD(s) = fieldDescriptor(s)
-- restr = restrictions                                               --
-- d     = data; used because "data" is a reserved keyword                                                          --
-- s     = strings; used to save space                                                                              --
-- i     = index; used to save space                                                                                --
-- {n}'  = second occurence of the attribute {n}; used to differentiate                                             --
----------------------------------------------------------------------------------------------------------------------
-- sample filePath = "C:\\skill\\src\\test\\resources\\genbinary\\auto\\accept\\age.sf"                             --
----------------------------------------------------------------------------------------------------------------------

type TD_v1 = (M'TypeID, Name, Count, M'SuperID, M'LBPO, LFieldCount, M'Restr)
type FD_v1 = (FieldID, M'Name, M'Getter, Int, M'B, M'B)
type FD_v2 = (FieldID, M'Name, M'Getter, FieldRecords')
type PartialState = ([String], [UserType], [TD_v2])

type M'B           = Maybe B.ByteString
type FieldRecords' = (RawData, M'TypeInfo, M'Restr)


deserialize :: FilePath -> IO ()
deserialize filePath = do state <- runGet process `fmap` C.readFile filePath
                          ImpossibleImports.modifyIORef' states ((:) state)

deserialize_ :: FilePath -> IO State
deserialize_ filePath = do state <- runGet process `fmap` C.readFile filePath
                           ImpossibleImports.modifyIORef' states ((:) state)
                           return state

initializeTest :: FilePath -> IO ()
initializeTest filePath = deserialize filePath >> head `fmap` readIORef states >>= printState

process :: Get State
process = g'file ([], [], []) >>= \(strings, uTs, tDs) -> return (strings, uTs, remake tDs)
             where remake = createHierarchy . upgradeFDs . compress


-------------------------------------
---------- DESERIALIZATION ----------
-------------------------------------

g'file :: PartialState -> Get PartialState
g'file (f_s, f_uTs, f_tDs_v2) = isEmpty >>= \e -> if e then return (f_s, f_uTs, f_tDs_v2)
 else do stringCount   <- g'v64
         offsets       <- g'Offsets stringCount
         s             <- g'strings offsets
         typeCount     <- g'v64
         (tDs_v1, uTs) <- g'tDs_v1 f_uTs (f_s ++ s) f_tDs_v2 typeCount
         fDs_v1        <- g'fDs_v1 (f_s ++ s) f_tDs_v2 tDs_v1       -- end of descriptive information
         typeData      <- g'data $ (map . map) (\(_,_,_,offset, _,_) -> offset) fDs_v1
         g'file (f_s ++ s, f_uTs ++ uTs, f_tDs_v2 ++ b'tDs_v2 tDs_v1 fDs_v1 typeData)


-- reads n many i32 values
g'Offsets :: Int -> Get [Int32]
g'Offsets = go 0
 where go _ 0 = return []
       go prevOffset i = getInt32be >>= \i32 -> go i32 (i-1) >>= \rest -> return (i32 - prevOffset : rest)

-- reads strings from the given offsets
g'strings :: [Int32] -> Get [String]
g'strings [] = return []
g'strings (offset : r_offsets) = do byteString <- getLazyByteString (fromIntegral offset)
                                    r_strings  <- g'strings r_offsets
                                    return (UTF8.toString byteString : r_strings)

-- reads the information about type descriptors from their position in the type block
g'tDs_v1 :: [UserType] -> [String] -> [TD_v2] -> Int -> Get ([TD_v1], [String])
g'tDs_v1 _ _ _ 0 = return ([],[])
g'tDs_v1 uTs strings f_tDs_v2 i
   = do name              <- g'refString strings
        let q1             = not $ any (\(_,name',_,_,_,_,_) -> name' == name) f_tDs_v2
        let n_uTs          = select q1 (uTs ++ [name])     uTs
        let id             = select q1 (Just (length uTs)) Nothing
        count             <- g'v64
        restr             <- g'm'typeRestrictions q1
        superID           <- maybeRead q1 g'index
        let q3             = count > 0 && superID `isGreater` (-1)
        lBPO              <- maybeRead q3 g'v64
        lFieldCount       <- g'v64
        (r_tDs_v1, f_uTs) <- g'tDs_v1 n_uTs strings f_tDs_v2 (i-1)
        return ((id, name, count, superID, lBPO, lFieldCount, restr) : r_tDs_v1, f_uTs)

-- g's field descriptors from the given offsets
g'fDs_v1 :: [String] -> [TD_v2] -> [TD_v1] -> Get [[FD_v1]]
g'fDs_v1 = go 0
  where go :: Int -> [String] -> [TD_v2] -> [TD_v1] -> Get [[FD_v1]]
        go _ _ _ [] = return []
        go latestOffset strings allDescs ((_,name,_,_,_,fieldCount,_) : r_tDs)
            = do let f_instancesOfCurrentType = filter (\(_,name',_,_,_,_,_) -> name' == name) allDescs
                 let fDsOfCurrentType         = concatMap (\(_,_,_,_,_,fDs,_) -> fDs) f_instancesOfCurrentType
                 let fieldIDsOfCurrentType    = map (\(fieldID,_,_,_) -> fieldID) fDsOfCurrentType
                 (fDsForInstance, offset)     <- g'fDsOfType strings latestOffset fieldIDsOfCurrentType fieldCount
                 r_fDs_v1                     <- go offset strings allDescs r_tDs
                 return (fDsForInstance : r_fDs_v1)

g'fDsOfType :: [String] -> Offset -> [FieldID] -> Int -> Get ([FD_v1], Int)
g'fDsOfType _ latestOffset _ 0 = return ([], latestOffset)
g'fDsOfType strings latestOffset allFieldIDsOfCurrentType fieldCount
  = do fieldID                    <- g'v64
       let q2                     = fieldID `notElem` allFieldIDsOfCurrentType
       m_name                     <- maybeRead q2 (g'refString strings)
       (m_source, m_getter)       <- unzipMaybe `fmap` maybeRead q2 (parseTypeDescription strings)
       restrictions               <- g'm'fieldRestrictions q2 m_getter
       offset                     <- g'v64
       (r_fDsOfType, finalOffset) <- g'fDsOfType strings offset allFieldIDsOfCurrentType (fieldCount - 1)
       --(FieldID, M'Name, M'Getter, Int, MB, MB)
       return ((fieldID, m_name, m_getter, offset - latestOffset, m_source, restrictions) : r_fDsOfType, finalOffset)
           where unzipMaybe :: Maybe (a, b) -> (Maybe a, Maybe b)
                 unzipMaybe (Just (a,b)) = (Just a, Just b)
                 unzipMaybe Nothing      = (Nothing, Nothing)

-- reads the data part of a type block. takes all (n*m) offsets as the argument
g'data :: [[Int]] -> Get [[FieldData]]
g'data [] = return []
g'data (offsets : r_offsets) = g'typeData offsets >>= \d -> g'data r_offsets >>= \r_d -> return (d : r_d)

-- reads the data corresponding to one type. takes its n offsets as the argument
g'typeData :: [Int] -> Get [FieldData]
g'typeData [] = return []
g'typeData (o : r_o) = getLazyByteString (fromIntegral o) >>= \d -> g'typeData r_o >>= \r_d -> return (d : r_d)

-- builds the next generation of type descriptors; stores field descriptors and data directly
-- e1 == id; e2 == name; e3 == count; e4 == superID; e5 == LBPO
b'tDs_v2 :: [TD_v1] -> [[FD_v1]] -> [[FieldData]] -> [TD_v2]
b'tDs_v2 [] [] [] = []
b'tDs_v2 ((e1,e2,e3,e4,e5, _, e7) : r_tDs_v1) (fDs_v1 : r_fDs_v1) (typeData : r_d)
       = (e1,e2,e3,e4,e5, b'fDs_v2 fDs_v1 typeData, e7) : b'tDs_v2 r_tDs_v1 r_fDs_v1 r_d


-- fieldID, name, getter, endOffset, typeInfo
-- fieldID, maybe name, maybe getter, (maybe typeInfo, maybe fieldData, maybe restrictions)

-- takes a list of the strings and a list of data chunks. attributes the data chunks correctly
b'fDs_v2 :: [FD_v1] -> [FieldData] -> [FD_v2]
b'fDs_v2 [] [] = []
b'fDs_v2 ((e1,e2,e3, _, m_typeInfo, m_restr) : r_fDs_v1) (d : r_d) = (e1,e2,e3, (d, m_typeInfo, m_restr)) : b'fDs_v2 r_fDs_v1 r_d

-------------------------------------
---------- TRANSFORMATIONS ----------
-------------------------------------

data TD_v5    = TD5 {
              typeId     :: TypeID,
              name       :: Name,
              count      :: Count,
              fDs        :: [FD],
              subtypes   :: [TD_v5],
              snips      :: [(Int, Int)],
              firstI     :: Int,
              baseFirstI :: Int,
              restr      :: Restr}
type TD_v4    = (TypeID, Name, Count, SuperID, M'LBPO, [FD], Restr) -- id, name, count, superID, maybe lbpo, fieldDescs
type TD_v3    = (TypeID, Name, Count, SuperID, M'LBPO, [FD_v2], Restr) -- id, name, count, superID, maybe lbpo, fD_v2s
type TD_v2    = (M'TypeID, Name, Count, M'SuperID, M'LBPO, [FD_v2], M'Restr) -- id, name, count, maybe superID, maybe lbpo, fieldDescs

-- merges type-extending descriptors with their originals, to create a 1 - 1 relation between types and descriptors
compress :: [TD_v2] -> [TD_v3]
compress = go []
  where go :: [TD_v3] -> [TD_v2] -> [TD_v3]
        go f_tDs [] = f_tDs
        go f_tDs ((Just id, e2, e3, Just superID, e5, e6, Just restr) : r_tDs) = go (f_tDs ++ [(id, e2, e3, superID, e5, e6, restr)]) r_tDs -- id exists -> original type
        go f_tDs ((Nothing, name, e3, e4, e5, e6, e7) : r_tDs) = go f_tDs' r_tDs -- id doesn't exist => type has to be merged
              where f_tDs' = replace' index ((f_tDs !! index) `mergeTDs` (Nothing, name, e3, e4, e5, e6, e7)) f_tDs
                    index  = findIndex' 0 name f_tDs
                       where findIndex' i name ((_,name',_,_,_,_,_) : r_tDs)
                                          | name == name' = i
                                          | otherwise     = findIndex' (i+1) name r_tDs

 -- merges an original type desc with an extending type desc
mergeTDs :: TD_v3 -> TD_v2 -> TD_v3
mergeTDs (e1, e2, count, e4, e5, fDs, e7) (_, _, count', _, _, fDs', _) = (e1, e2, count + count', e4, e5, fDs `mergeFDs` fDs', e7)

mergeFDs :: [FD_v2] -> [FD_v2] -> [FD_v2]
mergeFDs fDs [] = fDs
--mergeFDs fD1 fD2 = error (show fD1 ++ show fD2)
mergeFDs fDs (fD': r_fDs') = mergeFDs (go fDs fD') r_fDs'
  where go :: [FD_v2] -> FD_v2 -> [FD_v2]
        go [] fD' = [fD']
        go ((id, e2, e3, (rawD, e5, e6)) : r_fDs) (id', e2', e3',(rawD', e5', e6'))
           | id == id' = (id, e2, e3, (rawD `a` rawD', e5, e6)) : r_fDs
           | otherwise = (id, e2, e3, (rawD, e5, e6)) : go r_fDs (id', e2', e3',(rawD', e5', e6'))


upgradeFDs :: [TD_v3] -> [TD_v4]
upgradeFDs = map (\(e1, e2, count, e4, e5, fDs, e7) -> (e1, e2, count, e4, e5, map (go count) fDs, e7))
             where go :: Int -> FD_v2 -> FD
                   go count (_, Just name, Just getter, (d, Just typeInfo, Just restrictions))
                          = (name, runGet (repeatGet count getter) d, (d, typeInfo, restrictions))

-- turns the linear into a hierarchical structure, where subtypes are stored in attributes of super types
createHierarchy :: [TD_v4] -> [TD]
createHierarchy typeDescs = map convertTD_v5 $ go ([], typeDescs)
   where go :: ([TD_v5], [TD_v4]) -> [TD_v5]
         go (f_tDs, [])                                             = f_tDs
         go (f_tDs, tD : r_tDs)                                     = go (f_tDs `integrate` tD, r_tDs)
         convertTD_v5 (TD5 e1 e2 e3 e4 children _ _ _ e9) = TD e1 e2 e3 e4 (map convertTD_v5 children) e9

-- takes a list of processed, hierarchically ordered type descs and the to-process ne
-- data TD_v5    = TD5 (TypeID, Name, Count, [FD], [TD_v5], [(Int, Int)], Int, Int, TypeRecords) deriving (Show) -- id, name, count, fieldDescs, subtypes, snips, firstIndex, baseFirstIndex
-- type TD_v4    = (TypeID, Name, Count, SuperID, M'LBPO, [FD], TypeRecords) -- id, name, count, superID, maybe lbpo, fieldDescs
integrate :: [TD_v5] -> TD_v4 -> [TD_v5]
integrate f_tDs (e1,e2,e3, -1, _,e6,e7) = f_tDs ++ [TD5 e1 e2 e3 e6 [] [] 1 1 e7] --superID == -1 => add it on top level
integrate f_tDs tD_v4                   = case f_tDs `go` tD_v4
           of Just a  -> a
              Nothing -> error $ show tD_v4
                                                                --assure (f_tDs `go` tD_v4) -- superID > -1 => integrate it, no-failure mode
 where go :: [TD_v5] -> TD_v4 -> Maybe [TD_v5]
       go [] tD_v4    = Nothing -- right master not around -> FAILURE;
       go (tD_v5 : r_f_tDs_v5) tD_v4
        | id == supID = Just (tD_v5 `enslave` tD_v4 : r_f_tDs_v5) -- first tD is master -> enslave
        | otherwise   = case s_tDs `go` tD_v4                     -- first tD is not master -> try to give it to one of first's slaves
            of Just s_tDs' -> Just ((TD5 id name count fDs s_tDs' a6 a7 a8 records) : r_f_tDs_v5) -- success -> return tD with updated slaves
               Nothing     -> case r_f_tDs_v5 `go` tD_v4
                                  of Just r_f_tDs' -> Just (tD_v5 : r_f_tDs')
                                     Nothing       -> Nothing
          where TD5 id name count fDs s_tDs a6 a7 a8 records = tD_v5
                (id', e2, e3, supID, e5, e6, e7)             = tD_v4

-- takes a super and a sub type and stuffs the sub type into the children variable of the super type
enslave :: TD_v5 -> TD_v4 -> TD_v5
enslave tD_v5 (e1,e2, 0, e4, Nothing, e6,e7) = enslave tD_v5 (e1,e2, 0, e4, Just 0, e6, e7) -- count = 0 -> no lbpo; this works as the general case with LBPO 0
enslave (TD5 id name count fDs tDs snips f_index f_baseIndex rec)
             (id', name', count', _, Just lbpo', fDs', records')
 = TD5 id name (count - count') fd1 (tDs ++ [s_tD]) ((fixedLbpo, count') : snips) f_index f_baseIndex rec
      where s_tD            = TD5 id' name' count' (fd2 ++ fDs') [] [] (lbpo' + f_baseIndex) f_baseIndex records'
            (fd1, fd2)      = unzip $ map (split_fD adjustedOffsets) fDs
            adjustedOffsets = adjustOffsets (fixedLbpo, count') snips
            fixedLbpo       = lbpo' - f_index + 1

adjustOffsets :: (Int, Int) -> [(Int, Int)] -> (Int, Int)
adjustOffsets (offset, count) snips = go (offset, count) $ filter (\(offset',_) -> offset' < offset) snips
  where go (offset, count) snips = (offset - sum (map snd snips), count)

split_fD :: (Int, Int) -> FD -> (FD, FD)
split_fD (a,b) (n, d', record) = ((n, outer, record), (n, inner, (B.empty, B.empty, B.empty)))
  where (inner, outer) = (subList a b d', take a d' ++ drop (a+b) d')
