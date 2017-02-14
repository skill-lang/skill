module Serialize where

import qualified Data.ByteString.Lazy as B
import qualified Data.ByteString.Lazy.Char8 as W
import qualified Data.Map as M
import Data.List
import Methods
import Data.Binary.Put as P
import Data.Word
import Data.Maybe
import WriteFields
import Memory

-- a_ = amount of

--------------------------------------
---------- DETRANSFORMATION ----------
--------------------------------------

type M'Data = Maybe [Something]
type FieldRecords' = (B.ByteString, Maybe B.ByteString, Maybe B.ByteString)

data TD_S = TDS {typeId'   :: TypeID,
                 name'     :: Name,
                 count'    :: Count,
                 m'fDs'    :: [FD],
                 subtypes' :: [TD_S],
                 m'lbpo'   :: M'LBPO,
                 restr'    :: Restr} deriving Show


data TD_F = TDF {typeId   :: TypeID,
                 nameId   :: NameID,
                 count    :: Count,
                 fDs      :: [FD_F],
                 subtypes :: [TD_F],
                 m'lbpo   :: M'LBPO,
                 restr    :: Restr} deriving Show


type FD_S = (NameID, [Something], Data, Data)
type FD_F = (NameID, Data, Data, Data)

-- combines all necessary detransformations
detransform :: [String] -> [TD] -> ([TD_F], Int)
detransform strings tDs = (finalize (myMap, strings) tDs', a_tDs)
                where tDs'           = surface tDs
                      (myMap, a_tDs) = inferTypes tDs'

-- reverts some of the structural changes done at the end of the Deserialization process
-- leaves the hierarchy intact, but pulls the field data of inherited fields back up to
-- their highest-level definition and calculates a new local base pool offset.
-- Also creates new ByteStrings out of current field data.
surface :: [TD] -> [TD_S]
surface [] = []
surface (tD : tDs) = (deleteLbpo . head . fst'') (go 0 0 [tD]) : surface tDs
  -- go procedure is implemented for a list of sub types, the above routine abstracts this behavior
 where go :: LBPO -> Int -> [TD] -> ([TD_S], [[Something]], LBPO, Count)
       go lbpo _ [] = ([], [], lbpo, 0)
       go lbpo a_inhFields (TD id name count fDs subtypes restr : r_tDs) = (f_tDs, f_d, f_lbpo, f_count)
                -- amount inherited fields = number of fields that (transitively) belong to super types
                -- local base pool offset  = position at which instances of each type start to be serialized
          where (fDs'', d)                = chip a_inhFields fDs
                n_a_inhFields             = length fDs
                n_lbpo                    = select (null fDs) lbpo (lbpo + count)
                (f_subtypes, s_d, lbpo'', sub'cumul'count) = go n_lbpo n_a_inhFields subtypes -- call downwards
                n_count                   = sub'cumul'count + count
                -- at this point, we have gathered all data of super- and of current fields (from subtypes)
                -- the former needs to be passed upwards, the latter included to current fDs

                (s_d'', c_d)              = select (null s_d) ([], []) (splitAt a_inhFields s_d)           -- s_d'' := data to hand upward, c_d := data to keep
                f_fDs                     = refresh (upg'fDs fDs'' c_d)--refresh ((take a_inhFields fDs'') ++ upg'fDs (drop a_inhFields fDs'') c_d)  -- incorporate data to keep
                (r_tDs'', r_d, f_lbpo, sibling'cumul'count) = go lbpo'' a_inhFields r_tDs       -- call forwards (don't incorporate)
                f_count                   = n_count + sibling'cumul'count
                f_d                       = comprise (d, s_d'', r_d)          -- comprise all to-pass data
                m'lbpo                    = select (count == 0) Nothing (Just lbpo)
                f_tDs                     = TDS id name n_count f_fDs f_subtypes m'lbpo restr : r_tDs''

       -- adds data to the first (length data) fDs
       upg'fDs :: [FD] -> [[Something]] -> [FD]
       upg'fDs fDs [] = fDs
       upg'fDs ((name, d, record) : r_fDs) (d' : r_d') = (name, (d ++ d'), record) : upg'fDs r_fDs r_d'

       deleteLbpo :: TD_S -> TD_S
       deleteLbpo (TDS id name count fDs s_tDs _ rec) = TDS id name count fDs s_tDs Nothing rec

       comprise :: ([[Something]], [[Something]], [[Something]]) -> [[Something]]
       comprise (l1, l2, []) = comprise' (l1, l2)
       comprise (l1, [], l3) = comprise' (l1, l3)
       comprise ([], l2, l3) = comprise' (l2, l3)
       comprise (l1, l2, l3) = go (l1, l2, l3)
         where go ([], [], []) = []
               go ((a : x), (b : y), (c : z)) = (a ++ b ++ c) : go (x, y, z)

       comprise' :: ([[Something]], [[Something]]) -> [[Something]]
       comprise' (l1, []) = l1
       comprise' ([], l2) = l2
       comprise' (l1, l2) = go (l1, l2)
         where go ([], []) = []
               go ((a : x), (b : y)) = (a ++ b) : go (x, y)

       -- cuts the data of the first n fDs, returns (fDs with data removed, data)
       chip :: Int -> [FD] -> ([FD], [[Something]])
       chip 0 fDs                               = (fDs, []) -- remaining fDs have data...
       chip r ((e1, d, (rawD, e4, e5)) : r_fDs) = (f_fDs, d : r_d)
                   where (f_fDs, r_d)  = chip (r-1) r_fDs



inferTypes :: [TD_S] -> (M.Map Int Int, Int)
inferTypes = go 0 (M.singleton (-1) (-1))
  where go :: Int -> M.Map Int Int -> [TD_S] -> (M.Map Int Int, Int)
        go realIndex _ [] = (M.empty, realIndex)
        go realIndex myMap (TDS id e2 e3 e4 subtypes e6 e7 : r_tDs_s)
           = (M.insert id realIndex myMap `M.union` fst downward `M.union` fst forward, snd forward)
              where downward = go (realIndex + 1) M.empty subtypes
                    forward  = go (snd downward)  M.empty r_tDs_s

-- final transformation before the serialization process
-- updates superID's and Refs so that they refer to the correct implicit position
-- and turns strings back into string IDs.
finalize :: (M.Map Int Int, [String]) -> [TD_S] -> [TD_F]
finalize _ [] = []
finalize (myMap, strings) (TDS typeId name e3 fDs subtypes e6 e7 : r_tDs_s)
 = TDF (typeId `lookAndInc` myMap) (name `findAndInc` strings) e3 fDs' subtypes' e6 e7 : rest
     where fDs'      = map (go'fDs (myMap, strings)) fDs
           subtypes' = finalize (myMap, strings) subtypes
           rest      = finalize (myMap, strings) r_tDs_s

           go'fDs :: (M.Map Int Int, [String]) -> FD -> FD_F
           go'fDs (myMap, strings) (name, d, (rawD, typeInfo, restr))
              = (name `findAndInc` strings, runPut $ p'f'd (go (myMap, strings) d), typeInfo, restr)
                  where go :: (M.Map Int Int, [String]) -> [Something] -> [Something]
                        go (myMap, s) d
                         | isStrings d = map (\(GString string) -> (GInt (string `findAndInc` s))) d
                         | isRefs    d = map (\(GRef (i1, i2)) -> GRef (i1 `lookAndInc` myMap, i2)) d
                         | otherwise   = d
                             where isStrings ((GString _) : _) = True
                                   isStrings _                 = False
                                   isRefs    ((GRef _) : _)    = True
                                   isRefs _                    = False

-- Always add one, because id's in the binary file all start at 1 with 0 being error,
-- but ids during deserialization and usage start at 0
lookAndInc :: Int -> M.Map Int Int -> Int
lookAndInc k myMap = 1 + (assure' "lookAndInc" $ M.lookup k myMap)

findAndInc :: Eq a => a -> [a] -> Int
findAndInc v l = 1 + (assure $ elemIndex v l)




-----------------------------------
---------- VISUALIZATION ----------
-----------------------------------

print'tDs :: [TD_F] -> IO ()
print'tDs = mapM_ go
  where go (TDF id nameID count fDs subtypes lbpo rec)
          = putStrLn ("   ### Type " ++ show id ++ " : s(" ++ show nameID ++ ") LBPO " ++ show lbpo ++ " Count " ++ show count ++ " ###")
              >> eL 1 >> mapM_ print'fD fDs >> eL 1 >> putStr "Number of Subtypes: " >> print (length subtypes) >> print rec >> print'tDs subtypes


print'tDs_v1 :: [TD_S] -> IO ()
print'tDs_v1 = mapM_ go
  where go (TDS id name count fDs subtypes lbpo rec)
          = putStrLn ("   ### Type " ++ show id ++ " : " ++ name ++ " LBPO " ++ show lbpo ++ " Count " ++ show count ++ " ###")
              >> eL 1 >> mapM_ print'fD_v1 fDs >> eL 1 >> putStr "Number of Subtypes: " >> print (length subtypes) >> print'tDs_v1 subtypes


print'fD_v1 :: FD -> IO ()
print'fD_v1 (name, d, (rawD, _, _)) = putStr ("      > Field " ++ name ++ ": ") >> printData rawD
                                             >> putStr " -> " >> printList d >> eL 1

print'fD :: FD_F -> IO ()
print'fD (nameID, rawD, typeInfo, restr) = putStr ("      > Field s(" ++ show nameID ++ "): ") >> printData rawD
                                             >> putStrLn " -> ?"


-----------------------------------
---------- SERIALIZATION ----------
-----------------------------------

serialize :: State -> FilePath -> IO ()
serialize state filePath = W.writeFile filePath $ runPut (p'state state)

p'state :: State -> Put
p'state (s, _, tDs) = p'strings s >> p'tDs (tDs', a_tDs') >> p'fDs tDs' >> p'data tDs'
          where (tDs', a_tDs') = detransform s tDs

p'strings :: [String] -> Put
p'strings strings = p'v64 (length strings)
                 >> p'offsets (map (fromIntegral . length) strings)
                 >> mapM_ (\string -> putLazyByteString (W.pack string)) strings

p'offsets :: [Int] -> Put
p'offsets sizes = go 0 sizes
  where go _ [] = return ()
        go p_offset (size : r_sizes) = putInt32be (fromIntegral (size + p_offset))
                                    >> go (size + p_offset) r_sizes

p'tDs :: ([TD_F], Int) -> Put
p'tDs (tDs, a_tDs) = p'v64 a_tDs >> mapM_ (\tD -> go [tD] 0) tDs
  where go [] _ = return ()
        go (TDF id nameID count fDs s_tDs m_lbpo restr : r_tDs) superId
            = do p'v64 nameID
                 p'v64 count
                 putLazyByteString restr
                 p'v64 superId
                 p'm'v64 m_lbpo
                 p'v64 $ length fDs
                 go s_tDs id
                 go r_tDs superId


p'fDs :: [TD_F] -> Put                                                     -- all tDs
p'fDs tDs = go 0 tDs >> return ()
 where go :: Int -> [TD_F] -> PutM Int                                     -- all tDs passing through offset
       go offset [] = return offset
       go offset (tD : r_tDs)
          = do offset' <- go' offset tD
               go offset' r_tDs

       go' :: Int -> TD_F -> PutM Int                                      -- one top-level tD
       go' offset tD = do offset' <- go'' offset [tD]
                          return offset'

       go'' :: Int -> [TD_F] -> PutM (Int)                     -- one tD-hierarchy
       go'' offset [] = return (offset)
       go'' offset (TDF _ _ _ fDs subtypes _ _ : r_tDs)
          = do offset'  <- snd `fmap` go_fDs (1, offset) fDs -- handle fDs
               offset'' <- go'' offset' subtypes             -- call downward
               go'' offset'' r_tDs                           -- call forward

       go_fDs :: (Int, Int) -> [FD_F] -> PutM (Int, Int)     -- fDs of one tD
       go_fDs (id, offset) [] = return (id, offset)
       go_fDs (id, offset) (m'fD : m'fDs)
          = do (id', offset') <- go_fD (id, offset) m'fD     -- handle fD
               go_fDs (id', offset') m'fDs                   -- call forward

       go_fD :: (Int, Int) -> FD_F -> PutM (Int, Int)        -- one fD
       go_fD (id, offset) (nameID, rawD, typeInfo, restr)
          = do p'v64 id
               p'v64 nameID
               putLazyByteString typeInfo
               putLazyByteString restr
               p'v64 (offset + lengthBS rawD)
               return (id + 1, offset + lengthBS rawD)

p'data :: [TD_F] -> Put
p'data [] = return ()
p'data (TDF _ _ _ fDs subtypes _ _ : r_tDs) = mapM_ go fDs >> p'data subtypes >> p'data r_tDs
  where go (_, rawD, _, _) = putLazyByteString rawD
