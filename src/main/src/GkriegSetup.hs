module GkriegSetup where

import System.IO as IO
import Data.ByteString.Lazy.Char8 as C
import Data.List as L
import Data.ByteString.Lazy as S
import Data.Int
import Data.Binary.Get as G
import Data.Binary.Put as P
import Data.Char
import System.Random as R

roles = [4,4,4,4,3,2,1,1,1]
modifiers = [-3, -2, -1, 0, 0, 0, 1, 2, 2]

main = simulate 1000000 [0,0,0,0,0,0,0,0,0,0,0,0,0,0] >>= (print . addPairs)

addPairs :: [Int] -> [Int]
addPairs [] = []
addPairs (a : (b : rest)) = (a+b) : addPairs rest

simulate :: Int -> [Int] -> IO [Int]
simulate 0 results = return results
simulate runs results = do pos <- simOnce
                           let results' = inc (pos-1) results
                           simulate (runs-1) results'

inc :: Int -> [Int] -> [Int]
inc 0 (x : rest) = (x+1) : rest
inc i (x : rest) = x : inc (i-1) rest

simOnce :: IO Int
simOnce = go 0 0
 where go points dis
        | points >= 7 || dis >= 14 = return dis
        | even dis                 = R.randomRIO (0, 8) >>= \i -> go (points + (roles !! i)) (dis + 1)
        | otherwise                = R.randomRIO (0, 8) >>= \i -> go (points + (modifiers !! i)) (dis + 1)