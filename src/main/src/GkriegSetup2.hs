module GkriegSetup2 where

import System.IO as IO
import Data.List as L
import Data.Int
import System.Random as R

roles = [4,1,-4]
modifiers = [-3, -2, -1, 0, 0, 2, 2]

main = simulate (0, 0, 0, 0, 0) >>= print

simulate :: (Int, Int, Int, Int, Int) -> IO (Int, Int, Int, Int, Int)
simulate (a,b,c,d,e)
         | a + b + c + d + e == 100000 = return (a, b, c, d, e)
         | otherwise                   = simOnce >>= \results -> simulate (analyze a b c d e results)

analyze :: Int -> Int -> Int -> Int -> Int -> [Int] -> (Int, Int, Int, Int, Int)
analyze a b c d e results
          | length (filter (== (-4)) results) /= 1 = (a,b,c,d,e)
          | length results < 5                     = (a,b,c,d,e)
          | length results > 10                    = (a,b,c,d,e)
          | length (filter (== 4) results) > 1     = (a,b,c,d,e)
          | 4 `elem` results && length results > 8 = (a+1,b,c,d,e)
          | 4 `elem` results && length results > 6 = (a,b+1,c,d,e)
          | 4 `elem` results                       = (a,b,c+1,d,e)
          | length results > 6                     = (a,b,c,d+1,e)
          | otherwise                              = (a,b,c,d,e+1)

simOnce :: IO [Int]
simOnce = go []
 where go res
        | sum (map qq res) >= 7 || length res >= 14 = return res
        | even (length res)                         = R.randomRIO (0, 2) >>= \i -> go ((roles !! i) : res)
        | otherwise                                 = R.randomRIO (0, 6) >>= \i -> go ((modifiers !! i) : res)

qq :: Int -> Int
qq (-4) = 4
qq x  = x