-----------------------------------------------------------------------------
--
-- Module      :  WriteFields
-- Copyright   :
-- License     :  AllRightsReserved
--
-- Maintainer  :
-- Stability   :
-- Portability :
--
-- |
--
-----------------------------------------------------------------------------

module WriteFields where

import qualified Data.ByteString.Lazy as B
import Data.Int
import Data.Word
import Data.List
import Data.Binary.Put
import Methods
import Memory
import ImpossibleImports


p'm'v64 :: Maybe Int -> Put
p'm'v64 Nothing    = return ()
p'm'v64 (Just v64) = p'v64 v64

writeFloat = putInt32be . coerce
writeDouble = putInt64be . coerce

p'v64' :: Int64 -> Put
p'v64' = p'v64 . fromIntegral

p'v64 :: Int -> Put
p'v64 = go 1
  where go pos number
         | pos == 9 || number < 128 = putWord8 $ fromIntegral number
         | otherwise                = do putWord8 $ fromIntegral (128 + (number `mod` 128))
                                         go (pos + 1) (number `div` 128)
-- only called on lists of GStrings
writeStringRefs :: [String] -> [Something] -> Put
writeStringRefs s [] = return ()
writeStringRefs s ((GString string) : r_strings) = p'v64 $ assure (elemIndex string s)
writeStringRefs _ _ = error "writeStringRefs in WriteFields"

-- creates new raw Data
refresh :: [FD] -> [FD]
refresh = map go
  where go :: FD -> FD
        go (e1, d, (_, e4, e5)) = (e1, d, (runPut (p'f'd d), e4, e5))

-- serializes data
p'f'd :: [Something] -> Put
p'f'd [] = return ()
p'f'd ((GRef v) : r_d)    = mapM_ (\(GRef (_, v)) -> p'v64 (v+1)) ((GRef v) : r_d)
p'f'd ((GInt8 v) : r_d)   = mapM_ (\(GInt8 v)     -> putInt8 v)     ((GInt8 v) : r_d)
p'f'd ((GInt16 v) : r_d)  = mapM_ (\(GInt16 v)    -> putInt16be v)  ((GInt16 v) : r_d)
p'f'd ((GInt32 v) : r_d)  = mapM_ (\(GInt32 v)    -> putInt32be v)  ((GInt32 v) : r_d)
p'f'd ((GInt64 v) : r_d)  = mapM_ (\(GInt64 v)    -> putInt64be v)  ((GInt64 v) : r_d)
p'f'd ((GV64 v) : r_d)    = mapM_ (\(GV64 v)      -> p'v64' v)   ((GV64 v) : r_d)
p'f'd ((GFloat v) : r_d)  = mapM_ (\(GFloat v)    -> writeFloat v) ((GFloat v) : r_d)
p'f'd ((GDouble v) : r_d) = mapM_ (\(GDouble v)   -> writeFloat v) ((GDouble v) : r_d)
p'f'd ((GInt v) : r_d)    = mapM_ (\(GInt v)      -> p'v64 v) ((GInt v) : r_d)
p'f'd ((GFArray v) : r_d) = mapM_ (\(GFArray v)   -> p'f'd v) ((GFArray v) : r_d)
p'f'd ((GVArray v) : r_d) = p'v64 (length r_d + 1) >> mapM_ (\(GVArray v) -> p'f'd v) ((GVArray v) : r_d)
p'f'd ((GList v) : r_d)   = p'v64 (length r_d + 1) >> mapM_ (\(GList v) -> p'f'd v) ((GList v) : r_d)
p'f'd ((GSet v) : r_d)    = p'v64 (length r_d + 1) >> mapM_ (\(GSet v) -> p'f'd v) ((GSet v) : r_d)

--p'fieldData ((GFloat v) : r_d)
--p'fieldData ((GDouble v) : r_d)
--p'fieldData ((GInt v) : r_d) -- as supposed to GString
--p'fieldData ((GFArray v) : r_d)
--p'fieldData ((GVArray v) : r_d)
--p'fieldData ((GList v) : r_d)
--p'fieldData ((GSet v) : r_d)
--p'fieldData ((GMap v) : r_d)

