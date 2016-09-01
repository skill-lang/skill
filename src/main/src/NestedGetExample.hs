module NestedGetExample where

import System.IO as IO
import Data.ByteString.Lazy.Char8 as C
import Data.ByteString.Lazy as S
import Data.Binary.Get
import Data.Word
import qualified Data.List as L

processA :: Get (Get Word8, Word8, Word8)
processA = do a <- mA
              b <- getWord8
              c <- getWord8
              return (a, b, c)

processB :: Get (Get Word8, Word8, Word8)
processB = do a <- mB
              b <- getWord8
              c <- getWord8
              return (a, b, c)

processC :: Get (Get (Word8, Word8), Word8, Word8)
processC = do a <- mC
              b <- getWord8
              c <- getWord8
              return (a, b, c)

processD :: Get (Get (Word8, Word8), Word8, Word8)
processD = do a <- mD
              b <- getWord8
              c <- getWord8
              return (a, b, c)

processX :: Get (Get [Word8], Word8, Word8)
processX = do a <- mA
              let action = remodel $ L.replicate 5 a
              b <- getWord8
              c <- getWord8
              return (action, b, c)

main = do let string = C.pack "bcdef" -- 98, 99, 100, 101, 102
          let (a1, b1, c1) = runGet processA string
          let (a2, b2, c2) = runGet processB string
          let (a3, b3, c3) = runGet processC string
          let (a4, b4, c4) = runGet processD string
          let (ax, bx, cx) = runGet processX string
          print (b1, c1)
          print (b2, c2)
          print (b3, c3)
          print (b4, c4)
          print (bx, cx)
          IO.putStrLn "------"
          print $ runGet (makeGet a1) string
          print $ runGet (makeGet a2) string
          print $ runGet a3 string
          print $ runGet a4 string
          print $ runGet ax string




makeGet :: Get Word8 -> Get [Word8]
makeGet getter = do a <- getWord8
                    b <- getter
                    c <- getWord8
                    d <- getter
                    e <- getWord8
                    return [a,b,c,d,e]

mA :: Get (Get Word8)
mA = return (id `fmap` getWord8)

mB :: Get (Get Word8)
mB = return `fmap` getWord8

mC :: Get (Get (Word8, Word8))
mC = do a <- mA
        b <- mA
        c <- a
        d <- a
        (return . return) (c, d)

mD :: Get (Get (Word8, Word8))
mD = do a <- mA
        b <- mA
        let g = remodel' (a,b)
        return (id `fmap` g)

remodel :: [Get Word8] -> Get [Word8]
remodel [] = return []
remodel (getter : rest) = getter >>= \a -> remodel rest >>= \r -> return (a : r)

remodel' :: (Get Word8, Get Word8) -> Get (Word8, Word8)
remodel' (g1, g2) = g1 >>= \v1 -> g2 >>= \v2 -> return (v1, v2)