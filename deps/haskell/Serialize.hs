module Serialize where

import D_Types
import Data.List
import Methods

data R'TypeDesc  = R (Int, String, [FieldDesc], [R'TypeDesc]) deriving (Show) -- id, name, fieldDescs, subtypes
data TD_S = TDS (String, [FD_S], [TD_S], Maybe Int) deriving (Show)
type FD_S = (String, Maybe [Something])

c'tDs :: [TypeDesc] -> [R'TypeDesc]
c'tDs = map (\(TD (id, n, fDs, s_tDs)) -> R (id, n, c'fDs fDs, c'tDs s_tDs))
  where c'fDs = map (\(a,b,c) -> (a,b))

-- reverts some of the structural changes done at the end of the Deserialization process
-- leaves the hierarchy intact, but pulls the data of inherited fields back up to their highest-level definition
-- and calculates a new local base pool offset
surface :: [R'TypeDesc] -> [TD_S]
surface [] = []
surface (tD : tDs) = (deleteLbpo . head . fst') (go 0 0 [tD]) : surface tDs
  -- main procedure is implemented for a list of sub types, the above routine abstracts this behavior
  where go :: Int -> Int -> [R'TypeDesc] -> ([TD_S], [[Something]], Int)
        go lbpo _ [] = ([], [], lbpo)
        go lbpo a_inhFields ((R (_, name, fDs, subtypes) : r_tDs)) = (f_tDs, f_d, f_lbpo)
                -- amount inherited fields = number of fields that (transitively) belong to super types
                -- local base pool offset  = position at which instances of each type start to be serialized
          where (fDs'', d)                = chip a_inhFields fDs
                n_a_inhFields             = length fDs
                n_lbpo                    = select (null fDs) lbpo (lbpo + (length . snd . head) fDs)
                (f_subtypes, s_d, lbpo'') = go n_lbpo n_a_inhFields subtypes -- call downwards

                -- at this point, we have gathered all data of super- and of current fields (from subtypes)
                -- the former needs to be passed upwards, the latter included to current fDs

                (s_d'', c_d)              = select (null s_d) ([], []) (splitAt a_inhFields s_d)           -- s_d'' := data to hand upward, c_d := data to keep
                f_fDs                     = (take a_inhFields fDs'') ++ upg'fDs (drop a_inhFields fDs'') c_d                 -- incorporate data to keep
                (r_tDs'', r_d, f_lbpo)    = go lbpo'' a_inhFields r_tDs       -- call forwards (don't incorporate)
                f_d                       = comprise (d, s_d'', r_d)          -- comprise all to-pass data
                f_tDs                     = TDS (name, f_fDs, f_subtypes, Just lbpo) : r_tDs''




-- adds data to the first (length data) fDs
upg'fDs :: [FD_S] -> [[Something]] -> [FD_S]
upg'fDs fDs [] = fDs
upg'fDs ((name, Just d) : r_fDs) (d' : r_d') = (name, Just (d ++ d')) : upg'fDs r_fDs r_d'

deleteLbpo :: TD_S -> TD_S
deleteLbpo (TDS (name, fDs, s_tDs, _)) = TDS (name, fDs, s_tDs, Nothing)

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
chip :: Int -> [FieldDesc] -> ([FD_S], [[Something]])
chip 0 fDs                 = ((map (\(name, d) -> (name, Just d)) fDs), []) -- remaining fDs have data...
chip r ((name, d) : r_fDs) = (((name, Nothing) : f_fDs), (d : r_d))
            where (f_fDs, r_d) = chip (r-1) r_fDs

printTDS :: TD_S -> IO ()
printTDS (TDS (name, fDs, subtypes, lbpo))
            = eL 1 >> putStrLn ("   ### Type " ++ name ++ " LBPO " ++ show lbpo ++ " ###") >> eL 1 >> mapM_ p'fD fDs
              >> eL 1 >> putStr "Number of Subtypes: " >> print (length subtypes) >> mapM_ printTDS subtypes

p'fD :: FD_S -> IO ()
p'fD (name, Just d') = putStr ("      > Field " ++ name ++ ": ") >> printList (map show d')
p'fD (name, Nothing) = putStr ("      > Field " ++ name ++ ": " ++ "No Data")

