module NestedGetExample2 where

import System.IO as IO
import Data.ByteString.Lazy.Char8 as C
import Data.ByteString.Lazy as S
import Data.Binary.Get
import Data.Word
import Data.Int
import qualified Data.List as L
import qualified Methods as M
import ReadFields
import Types


main = do string <- C.readFile "C:\\BinaryFiles\\nestedGet.ex" -- 17, 07, 03, 16, 32, 48, 64 80
          let (str1, str2) = C.splitAt 2 string
          printBytes str1
          printBytes str2
          let getter = runGet parseTypeDescription str1
          let res    = runGet getter str2
          printSomething res
          IO.putStrLn "done"



readWord8s :: Int64 -> Get [Word8]
readWord8s 0 = return []
readWord8s i = getWord8 >>= \w -> readWord8s (i-1) >>= \r -> return (w : r)

printBytes :: ByteString -> IO ()
printBytes string = print $ runGet (readWord8s (C.length string)) string


printSomething :: Something -> IO ()
printSomething (CInt8 a)   = print a
printSomething (CInt16 a)  = print a
printSomething (CInt32 a)  = print a             -- 2
printSomething (CInt64 a)  = print a             -- 3
printSomething (CV64   a)  = print a
printSomething (GBool  a)  = print a
printSomething (GInt8  a)  = print a
printSomething (GInt16 a)  = print a
printSomething (GInt32 a)  = print a
printSomething (GInt64 a)  = print a
printSomething (GV64   a)  = print a
printSomething (GFloat a)  = print a
printSomething (GDouble a) = print a
printSomething (GString a) = print a
printSomething (GFArray a) = IO.putStr "[" >> mapM_ printSomething a >> IO.putStr "]"
printSomething (GVArray a) = IO.putStr "[" >> mapM_ printSomething a >> IO.putStr "]"
printSomething (GList a)   = error "GList"
printSomething (GSet a)    = error "GSet"       -- 19